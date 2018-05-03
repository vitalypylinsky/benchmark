package com.example.benchmark.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource")
    public DataSource appDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Qualifier("sources")
    public List<JdbcTemplate> sources(PropConfig propConfig) {
        if (propConfig.getDriverClassName() == null) {
            return Collections.emptyList();
        }
        int nSources = propConfig.getDriverClassName().size();
        List<JdbcTemplate> sources = new ArrayList<>(nSources);
        IntStream.range(0, nSources)
                .forEach(idx ->
                        sources.add(new JdbcTemplate(DataSourceBuilder
                                .create()
                                .driverClassName(propConfig.getDriverClassName().get(idx))
                                .url(propConfig.getUrl().get(idx))
                                .username(propConfig.getUsername().get(idx))
                                .password(propConfig.getPassword().get(idx))
                                .build()))
                );
        return sources;
    }
}