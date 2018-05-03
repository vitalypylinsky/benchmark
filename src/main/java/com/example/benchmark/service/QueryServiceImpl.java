package com.example.benchmark.service;

import com.example.benchmark.config.PropConfig;
import com.example.benchmark.dao.VersionRepository;
import com.example.benchmark.dao.QueryRepository;
import com.example.benchmark.dao.RunRepository;
import com.example.benchmark.model.Version;
import com.example.benchmark.model.Query;
import com.example.benchmark.model.Run;
import com.example.benchmark.model.Status;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

@Service
public class QueryServiceImpl implements QueryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private PropConfig props;

    private final List<ExecutorService> executors;

    private final List<JdbcTemplate> sources;

    private final Lock reqOrderLock = new ReentrantLock();
    private final Lock createOrUpdateLock = new ReentrantLock();

    @Autowired
    public QueryServiceImpl(@Qualifier("sources") List<JdbcTemplate> sources) {
        this.sources = sources;
        int N_SOURCES = sources.size();
        executors = new ArrayList<>(N_SOURCES);
        IntStream.range(0, N_SOURCES)
                .forEach(idx -> {
                            // single Thread Executor per DB, 
                            // only one thread is active at any point of time against a DB
                            executors.add(Executors.newSingleThreadExecutor());
                        }
                );
    }

    @Override
    public List<Query> listQueries(Pageable pageable) {
        logger.info("listQueries called");
        return queryRepository.findAll(pageable).getContent();
    }

    @Override
    public Query findQuery(Long id) {
        logger.info("getQuery by id called");
        return queryRepository.findById(id).orElse(null);
    }

    @Override
    public Query findQuery(String queryName) {
        logger.info("getQuery by name called");
        return queryRepository.findByName(queryName).orElse(null);
    }

    @Override
    public List<Version> getQueryVersions(Query query, Pageable pageable) {
        return versionRepository.findByQueryOrderByCreatedDesc(query, pageable);
    }

    @Override
    public Query createOrUpdateQuery(String name, String txt) {
        logger.info("createOrUpdateQuery called");
        Query query = null;
        // make the operation atomic to prevent multiple creation of the same query
        createOrUpdateLock.lock();
        try {
            query = findQuery(name);
            if (Objects.isNull(query)) {
                query = queryRepository.save(new Query(null, name));
            }
        } finally {
            createOrUpdateLock.unlock();
        }
        measureQuery(query, txt);
        return query;
    }

    @Override
    public void executeQuery(Query query) {
        logger.info("executeQuery called");
        List<Version> versions = versionRepository.findByQueryOrderByCreatedDesc(query, PageRequest.of(0, 1));
        if (!versions.isEmpty()) {
            measureQuery(query, versions.get(0).getTxt());
        }
    }

    public void measureQuery(Query query, final String sql) {
        logger.info(sources.toString());
        logger.info("sql = [" + sql + "]");

        // lock to form correct order to executors - parallel requests don't mix up
        reqOrderLock.lock();
        try {

            final Version version = versionRepository
                    .save(new Version(null, sql, LocalDateTime.now(), query, Collections.emptyList()));

            IntStream.range(0, sources.size()).forEach(idx -> {

                final Run run = runRepository.save(new Run(null, getDbUrl(idx), Status.SCHEDULED, 0L, version));

                executors.get(idx).execute(() -> {

                    logger.info(String.format("----- Query name: %s, version id: %d, run %d %s -----",
                            version.getQuery().getName(), version.getId(), idx, Status.EXECUTING));
                    run.setStatus(Status.EXECUTING);
                    runRepository.save(run);

                    JdbcTemplate jdbcTemplate = sources.get(idx);

                    long startTime = System.currentTimeMillis();
                    try {
                        jdbcTemplate.execute(sql);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.info(String.format("----- Query name: %s, version id: %d, run %d %s -----",
                                version.getQuery().getName(), version.getId(), idx, Status.FAILED));
                        run.setStatus(Status.FAILED);
                        runRepository.save(run);
                        return;
                    }

                    long stopTime = System.currentTimeMillis();
                    long elapsedTime = stopTime - startTime;
                    logger.info(String.format("----- Query name: %s, version id: %d, run %d, work time: %d ms %s -----",
                            version.getQuery().getName(), version.getId(), idx, elapsedTime, Status.DONE));
                    run.setStatus(Status.DONE);
                    run.setWorkTime(elapsedTime);
                    runRepository.save(run);
                });
            });
        } finally {
            reqOrderLock.unlock();
        }
    }

    private String getDbUrl(int idx) {
        String dbUrl = String.format("DB installation %d", idx);
        if (!Objects.isNull(props.getUrl())) {
            dbUrl = props.getUrl().get(idx);
        }
        return dbUrl;
    }

    @Override
    public void deleteQuery(Long id) {
        logger.info("deleteQuery called");
        Optional<Query> query = queryRepository.findById(id);
        query.ifPresent(q -> {
            versionRepository.deleteAll(versionRepository.findByQuery(q));
            queryRepository.deleteById(id);
        });
    }

    @Override
    public void deleteAll() {
        logger.info("deleteAll called");
        listQueries(Pageable.unpaged()).forEach(q -> deleteQuery(q.getId()));
    }

    @Override
    public Version getVersion(Long id) {
        logger.info("getVersion called");
        return versionRepository.findById(id).orElse(null);
    }

    @Override
    public Run getRun(Long id) {
        logger.info("getRun called");
        return runRepository.findById(id).orElse(null);
    }
}