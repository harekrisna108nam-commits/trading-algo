package com.example.dhan_rsi_series.service;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.dhan_rsi_series.model.Candle;
import com.example.dhan_rsi_series.model.HistoricalCandleRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DhanMarketDataService {

    private final WebClient dhanWebClient;

    public List<Candle> fetchCandles(String securityId, int interval) {

        return dhanWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/charts/intraday")
                        .queryParam("securityId", securityId)
                        .queryParam("interval", interval)
                        .build())
                .retrieve()
                .bodyToFlux(Candle.class)
                .collectList()
                .block();
    }
    
    public Mono<Candle> fetchIntradayCandles(HistoricalCandleRequest request) {

        return dhanWebClient.post()
                .uri("/v2/charts/intraday")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Candle.class);
    }

}
