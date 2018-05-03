package com.example.benchmark.controller;

import com.example.benchmark.dto.*;
import com.example.benchmark.model.Version;
import com.example.benchmark.model.Query;
import com.example.benchmark.model.Run;
import com.example.benchmark.service.QueryService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/query")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @ApiOperation(value = "View a list of available queries")
    @GetMapping(value = "/list", produces = "application/json")
    public List<QueryDto> listQueries(Pageable pageable) {
        List<Query> queries = queryService.listQueries(pageable);
        return queries.stream().map(QueryDto::new).collect(toList());
    }

    @ApiOperation(value = "Search a query with an ID")
    @GetMapping(value = "/findById/{id}", produces = "application/json")
    public QueryResponse findQuery(@PathVariable Long id, Pageable pageable) {
        Query query = queryService.findQuery(id);
        return getQueryResponse(query, pageable);
    }

    @ApiOperation(value = "Search a query with a name")
    @GetMapping(value = "/findByName/{queryName}", produces = "application/json")
    public QueryResponse findQuery(@PathVariable String queryName, Pageable pageable) {
        Query query = queryService.findQuery(queryName);
        return getQueryResponse(query, pageable);
    }

    @ApiOperation(value = "Create/update a query - triggers execution")
    @PostMapping(value = "/createOrUpdate", produces = "application/json")
    public QueryResponse createOrUpdateQuery(@RequestBody NewQueryRequest request, Pageable pageable) {
        Query query = queryService.createOrUpdateQuery(request.getName(), request.getTxt());
        return getQueryResponse(query, pageable);
    }

    @ApiOperation(value = "Execute the query's latest version against different DB installations")
    @GetMapping(value = "/execute/{queryName}", produces = "application/json")
    public QueryResponse executeQuery(@PathVariable String queryName, Pageable pageable) {
        Query query = queryService.findQuery(queryName);
        if (!Objects.isNull(query)) {
            queryService.executeQuery(query);
        }
        return getQueryResponse(query, pageable);
    }

    private QueryResponse getQueryResponse(@Nullable Query query, Pageable pageable) {
        if (Objects.isNull(query)) {
            return null;
        }
        List<Version> qVersions = queryService.getQueryVersions(query, pageable);
        return new QueryResponse(query, qVersions);
    }

    @ApiOperation(value = "Delete the query and its versions")
    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    public ResponseEntity deleteQuery(@PathVariable Long id) {
        queryService.deleteQuery(id);
        return new ResponseEntity("Query has been deleted successfully", HttpStatus.OK);
    }

    @ApiOperation(value = "Delete all queries and versions")
    @DeleteMapping(value = "/deleteAll", produces = "application/json")
    public ResponseEntity deleteAllQueries() {
        queryService.deleteAll();
        return new ResponseEntity("Queries have been deleted successfully", HttpStatus.OK);
    }

    @ApiOperation(value = "Show the specified version")
    @GetMapping(value = "/showVersion/{id}", produces = "application/json")
    public VersionDto showVersion(@PathVariable Long id) {
        Version version = queryService.getVersion(id);
        if (Objects.isNull(version)) {
            return null;
        }
        return new VersionDto(version);
    }

    @ApiOperation(value = "Show the specified DB run")
    @GetMapping(value = "/showRun/{id}", produces = "application/json")
    public RunDto showRun(@PathVariable Long id) {
        Run run = queryService.getRun(id);
        if (Objects.isNull(run)) {
            return null;
        }
        return new RunDto(run);
    }
}