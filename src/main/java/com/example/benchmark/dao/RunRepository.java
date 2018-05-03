package com.example.benchmark.dao;

import com.example.benchmark.model.Run;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunRepository extends JpaRepository<Run, Long> {

}