package com.example.benchmark.service;

import com.example.benchmark.model.Version;
import com.example.benchmark.model.Query;
import com.example.benchmark.model.Run;
import com.example.benchmark.model.Version;
import org.springframework.data.domain.Pageable;


import java.util.List;

public interface QueryService {

    List<Query> listQueries(Pageable pageable);

    Query findQuery(Long id);

    Query findQuery(String queryName);

    List<Version> getQueryVersions(Query query, Pageable pageable);

    Query createOrUpdateQuery(String name, String txt);

    void executeQuery(Query query);

    void deleteQuery(Long id);

    void deleteAll();

    Version getVersion(Long id);

    Run getRun(Long id);
}