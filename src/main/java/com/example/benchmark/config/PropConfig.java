package com.example.benchmark.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("spring.data")
@Data
public class PropConfig {

    private List<String> driverClassName;
    private List<String> url;
    private List<String> username;
    private List<String> password;

}