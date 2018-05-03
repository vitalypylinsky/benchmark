package com.example.benchmark.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class NewQueryRequest {

    @ApiModelProperty(notes = "The query name of a new query")
    private String name;

    @ApiModelProperty(notes = "The content of the new query")
    private String txt;

    public void setName(String name) {
        this.name = name;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

}