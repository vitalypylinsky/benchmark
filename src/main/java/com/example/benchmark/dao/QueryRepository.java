package com.example.benchmark.dao;

import com.example.benchmark.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface QueryRepository extends JpaRepository<Query, Long> {

    Optional<Query> findByName(String queryName);
}