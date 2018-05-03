package com.example.benchmark.dto;

import com.example.benchmark.model.Run;
import com.example.benchmark.model.Status;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@Getter
public class RunDto {

    @ApiModelProperty(notes = "The database generated query version run ID")
    private Long id;

    @ApiModelProperty(notes = "The jdbc url of DB where to preform the run")
    private String jdbcUrl;

    @ApiModelProperty(notes = "The status of the run")
    private Status status;

    @ApiModelProperty(notes = "The working time of the run")
    private Long workTime;


    public RunDto(Run run) {
        this.id = run.getId();
        this.jdbcUrl = run.getJdbcUrl();
        this.status = run.getStatus();
        this.workTime = run.getWorkTime();
    }

}