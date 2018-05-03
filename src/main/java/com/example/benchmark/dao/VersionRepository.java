package com.example.benchmark.dao;

import com.example.benchmark.model.Version;
import com.example.benchmark.model.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionRepository extends JpaRepository<Version, Long> {

    List<Version> findByQuery(Query query);

    List<Version> findByQueryOrderByCreatedDesc(Query query);

    List<Version> findByQueryOrderByCreatedDesc(Query query, Pageable pageable);
}