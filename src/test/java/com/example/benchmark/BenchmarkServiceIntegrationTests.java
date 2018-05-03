package com.example.benchmark;

import com.example.benchmark.dto.NewQueryRequest;
import com.example.benchmark.model.Status;
import com.example.benchmark.service.QueryService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class BenchmarkServiceIntegrationTests {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private QueryService service;

    // number of DB installations
    private static final int N_SOURCES = 2;
    // duration of one DB run, seconds
    private static final int EXEC_TIME_COEFF = 5;
    // DB load coefficient: 0 means queries will take the same time
    private static int db_equality_coeff = 0;


    /*Inner test configuration class for stubbing DB installations
    * and simulating different DB load*/
    @TestConfiguration
    static class ContextConfiguration {

        @Bean
        @Primary
        @Qualifier("sources")
        public List<JdbcTemplate> sources() {
            List<JdbcTemplate> sources = new ArrayList<>(N_SOURCES);
            IntStream.range(0, N_SOURCES)
                    .forEach(idx ->
                            sources.add(new JdbcTemplate() {
                                            @Override
                                            public void execute(String sql) throws DataAccessException {
                                                try {
                                                    long secs = (idx * db_equality_coeff + 1) * EXEC_TIME_COEFF;
                                                    logger.info(String.format("Running query for %d seconds", secs));
                                                    TimeUnit.SECONDS.sleep(secs);
                                                } catch (InterruptedException e) {
                                                }
                                            }
                                        }
                            ));
            return sources;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    public static final String QUERY_NAME = "Test";
    public static final String QUERY_TXT = "select * from test";

    @Before
    public void setUp() {
        // DBs are equal by default, i.e. all queries execute equally
        db_equality_coeff = 0;
    }

    @After
    public void tearDown() throws Exception {
        this.mockMvc.perform(delete("/query/deleteAll"));
        TimeUnit.SECONDS.sleep(EXEC_TIME_COEFF);
    }

    @Test
    public void shouldReturnEmptyList() throws Exception {
        this.mockMvc.perform(get("/query/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }


    @Test
    public void runOneQuery() throws Exception {
        this.mockMvc.perform(post("/query/createOrUpdate")
                .content(this.json(new NewQueryRequest(QUERY_NAME, QUERY_TXT)))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())))
                .andExpect(jsonPath("$.versions[0].runs", empty()));

        // wait till all runs are done
        // since the runs happen in parallel, the total execution time should not exceed EXEC_TIME_COEFF
        TimeUnit.SECONDS.sleep(EXEC_TIME_COEFF);
    }

    @Test
    public void shouldHaveOneQuery() throws Exception {
        runOneQuery();
        this.mockMvc.perform(get("/query/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void validateOneQuery() throws Exception {
        runOneQuery();
        // check all is done
        this.mockMvc.perform(get("/query/findByName/{name}", QUERY_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[0].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[0].runs", hasSize(N_SOURCES)))
                .andExpect(jsonPath("$.versions[0].runs[0].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[0].runs[1].status", is(Status.DONE.name())));
    }

    @Test
    public void editOneQueryAndValidate() throws Exception {
        runOneQuery();
        this.mockMvc.perform(post("/query/createOrUpdate")
                .content(this.json(new NewQueryRequest(QUERY_NAME, QUERY_TXT + "xxx")))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions", hasSize(2)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT + "xxx")))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())))
                .andExpect(jsonPath("$.versions[0].runs", empty()))
                .andExpect(jsonPath("$.versions[1].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[1].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[1].runs", hasSize(N_SOURCES)));

        // wait till all runs are done
        // since the runs happen in parallel, the total execution time should not exceed EXEC_TIME_COEFF
        TimeUnit.SECONDS.sleep(EXEC_TIME_COEFF);

        this.mockMvc.perform(get("/query/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // check all is done
        this.mockMvc.perform(get("/query/findByName/{name}", QUERY_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions", hasSize(2)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT + "xxx")))
                .andExpect(jsonPath("$.versions[0].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[0].runs", hasSize(N_SOURCES)))
                .andExpect(jsonPath("$.versions[0].runs[0].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[0].runs[1].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[1].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[1].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[1].runs", hasSize(N_SOURCES)));
    }

    @Test
    public void executeTheLatestVersionOfQuery() throws Exception {
        runOneQuery();
        this.mockMvc.perform(get("/query/execute/{name}", QUERY_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions", hasSize(2)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())))
                .andExpect(jsonPath("$.versions[0].runs", empty()))
                .andExpect(jsonPath("$.versions[1].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[1].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[1].runs", hasSize(N_SOURCES)));

        // wait till all runs are done
        // since the runs happen in parallel, the total execution time should not exceed EXEC_TIME_COEFF
        TimeUnit.SECONDS.sleep(EXEC_TIME_COEFF);

        this.mockMvc.perform(get("/query/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

    }

    @Test
    public void runTwoQueries() throws Exception {
        this.mockMvc.perform(post("/query/createOrUpdate")
                .content(this.json(new NewQueryRequest(QUERY_NAME, QUERY_TXT)))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())))
                .andExpect(jsonPath("$.versions[0].runs", empty()));

        this.mockMvc.perform(post("/query/createOrUpdate")
                .content(this.json(new NewQueryRequest(QUERY_NAME + "2", QUERY_TXT + "2")))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME + "2")))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT + "2")))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())))
                .andExpect(jsonPath("$.versions[0].runs", empty()));

        // wait till all runs are done
        // since the runs happen in parallel per Query, the total execution time should not exceed 2 * EXEC_TIME_COEFF
        TimeUnit.SECONDS.sleep(2 * EXEC_TIME_COEFF);
    }

    @Test
    public void runTwoQueriesAndValidate() throws Exception {
        runTwoQueries();

        this.mockMvc.perform(get("/query/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // check all is done for the 1st query
        this.mockMvc.perform(get("/query/findByName/{name}", QUERY_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[0].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[0].runs", hasSize(N_SOURCES)))
                .andExpect(jsonPath("$.versions[0].runs[0].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[0].runs[1].status", is(Status.DONE.name())));

        // check all is done for the 2nd query
        this.mockMvc.perform(get("/query/findByName/{name}", QUERY_NAME + "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME + "2")))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT + "2")))
                .andExpect(jsonPath("$.versions[0].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[0].runs", hasSize(N_SOURCES)))
                .andExpect(jsonPath("$.versions[0].runs[0].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[0].runs[1].status", is(Status.DONE.name())));
    }

    @Test
    public void runTwoQueriesOnlyOneIsExecutingAtTheMoment() throws Exception {
        this.mockMvc.perform(post("/query/createOrUpdate")
                .content(this.json(new NewQueryRequest(QUERY_NAME, QUERY_TXT)))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())))
                .andExpect(jsonPath("$.versions[0].runs", empty()));

        this.mockMvc.perform(post("/query/createOrUpdate")
                .content(this.json(new NewQueryRequest(QUERY_NAME + "2", QUERY_TXT + "2")))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME + "2")))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT + "2")))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())))
                .andExpect(jsonPath("$.versions[0].runs", empty()));

        // wait half time and check only one is executing at the moment
        TimeUnit.SECONDS.sleep(EXEC_TIME_COEFF / 2);

        this.mockMvc.perform(get("/query/findByName/{name}", QUERY_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions[0].status", is(Status.EXECUTING.name())));

        this.mockMvc.perform(get("/query/findByName/{name}", QUERY_NAME + "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME + "2")))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())));

        TimeUnit.SECONDS.sleep(EXEC_TIME_COEFF / 2);
    }

    /* There is a separate SingleThread pool for each DB installation.
    * So, if there is a DB that works faster than others then the second query can start its execution on the DB
    * once the first query on that DB is done. Even when runs of the first query on other DBs are not ready.*/
    @Test
    public void runTwoQueriesOnDBsWithDifferentLoad() throws Exception {
        db_equality_coeff = 1;
        this.mockMvc.perform(post("/query/createOrUpdate")
                .content(this.json(new NewQueryRequest(QUERY_NAME, QUERY_TXT)))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT)))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())))
                .andExpect(jsonPath("$.versions[0].runs", empty()));

        this.mockMvc.perform(post("/query/createOrUpdate")
                .content(this.json(new NewQueryRequest(QUERY_NAME + "2", QUERY_TXT + "2")))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME + "2")))
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].txt", is(QUERY_TXT + "2")))
                .andExpect(jsonPath("$.versions[0].status", is(Status.SCHEDULED.name())))
                .andExpect(jsonPath("$.versions[0].runs", empty()));

        // wait the run on the first DB is done for the first query
        TimeUnit.SECONDS.sleep(EXEC_TIME_COEFF);

        this.mockMvc.perform(get("/query/findByName/{name}", QUERY_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME)))
                .andExpect(jsonPath("$.versions[0].status", is(Status.EXECUTING.name())))
                .andExpect(jsonPath("$.versions[0].runs[0].status", is(Status.DONE.name())))
                .andExpect(jsonPath("$.versions[0].runs[1].status", is(Status.EXECUTING.name())));

        this.mockMvc.perform(get("/query/findByName/{name}", QUERY_NAME + "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query.name", is(QUERY_NAME + "2")))
                .andExpect(jsonPath("$.versions[0].runs[0].status", is(Status.EXECUTING.name())))
                .andExpect(jsonPath("$.versions[0].runs[1].status", is(Status.SCHEDULED.name())));

        TimeUnit.SECONDS.sleep(EXEC_TIME_COEFF);
    }

    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream()
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);

        assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

}
