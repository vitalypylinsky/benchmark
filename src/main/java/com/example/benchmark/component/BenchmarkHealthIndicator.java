package com.example.benchmark.component;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
class BenchmarkHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.status("Okay!").build();
    }
}