package com.example.benchmark.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@Data
public class Version {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 1024)
    private String txt;

    private LocalDateTime created;

    public Version() {
        created = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "fk_query", nullable = false)
    private Query query;

    @OneToMany(mappedBy = "version", orphanRemoval = true)
    private List<Run> runs;

}