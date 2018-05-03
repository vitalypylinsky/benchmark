package com.example.benchmark.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Run {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String jdbcUrl;

    @Enumerated(EnumType.STRING)
    private Status status;

    @NotNull
    private Long workTime;

    @ManyToOne
    @JoinColumn(name = "fk_version", nullable = false)
    private Version version;
}