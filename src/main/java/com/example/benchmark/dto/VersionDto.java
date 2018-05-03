package com.example.benchmark.dto;

import com.example.benchmark.model.Version;
import com.example.benchmark.model.Status;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Getter
public class VersionDto {

    @ApiModelProperty(notes = "The database generated query version ID")
    private Long id;

    @ApiModelProperty(notes = "The content of the query, code itself")
    private String txt;

    @ApiModelProperty(notes = "The timestamp of the query version")
    private LocalDateTime created;

    @ApiModelProperty(notes = "The list of runs associated with the query version")
    private List<RunDto> runs;

    @ApiModelProperty(notes = "The status of the query version execution")
    private Status status;

    public VersionDto(Version version) {
        this.id = version.getId();
        this.txt = version.getTxt();
        this.created = version.getCreated();
        runs = version.getRuns().stream().map(RunDto::new).collect(toList());
        status = calculateStatus(runs);
    }

    private Status calculateStatus(List<RunDto> runs) {
        Status vStatus = Status.SCHEDULED;
        if (!runs.isEmpty() && runs.stream().allMatch(r -> r.getStatus() == Status.DONE)) {
            vStatus = Status.DONE;
        }
        if (runs.stream().anyMatch(r -> r.getStatus() == Status.EXECUTING)) {
            vStatus = Status.EXECUTING;
        }
        if (runs.stream().anyMatch(r -> r.getStatus() == Status.FAILED)) {
            vStatus = Status.FAILED;
        }
        return vStatus;
    }
}