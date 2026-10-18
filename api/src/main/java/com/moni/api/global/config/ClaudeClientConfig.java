package com.moni.api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClaudeClientConfig {

    private static final String ANTHROPIC_BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Bean
    public RestClient claudeRestClient() {
        return RestClient.builder()
                .baseUrl(ANTHROPIC_BASE_URL)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("content-type", "application/json")
                .build();
    }
}
