package com.example.benchmark.dto;

import com.example.benchmark.model.Version;
import com.example.benchmark.model.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Getter
public class QueryResponse {

    @ApiModelProperty(notes = "The query to display")
    private QueryDto query;

    @ApiModelProperty(notes = "The list of query versions")
    List<VersionDto> versions;

    public QueryResponse(Query query, List<Version> versions) {
        this.query = new QueryDto(query);
        this.versions = versions.stream().map(VersionDto::new).collect(toList());
    }

}