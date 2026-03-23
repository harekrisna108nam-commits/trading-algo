package com.example.dhan_rsi_series.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.dhan_rsi_series.model.FundLimitResponse;

import reactor.core.publisher.Mono;

@Service
public class DhanFundLimitService {

    @Autowired
    private WebClient dhanWebClient;

    public Mono<FundLimitResponse> getFundLimit() {

    	return dhanWebClient.get()
                .uri("/v2/fundlimit")
                .header("Content-Type", "application/json")
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                    response.bodyToMono(String.class)
                            .flatMap(error -> Mono.error(new RuntimeException(error)))
                )
                .bodyToMono(FundLimitResponse.class);
    }
}