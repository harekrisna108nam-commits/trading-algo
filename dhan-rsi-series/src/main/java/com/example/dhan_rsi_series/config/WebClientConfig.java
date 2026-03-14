package com.example.dhan_rsi_series.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${dhan.base-url}")
    private String baseUrl;

    @Value("${dhan.access-token}")
    private String accessToken;

    @Bean
    public WebClient dhanWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("access-token", accessToken)
                .build();
    }
}
