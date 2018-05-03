
package com.example.benchmark.dto;

import com.example.benchmark.model.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@Getter
public class QueryDto {

    @ApiModelProperty(notes = "The database generated query ID")
    private Long id;

    @ApiModelProperty(notes = "The query name")
    private String name;

    public QueryDto(Query query) {
        this.id = query.getId();
        this.name = query.getName();
    }

}