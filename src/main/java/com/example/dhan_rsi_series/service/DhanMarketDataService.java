package com.example.dhan_rsi_series.service;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.dhan_rsi_series.model.Candle;
import com.example.dhan_rsi_series.model.HistoricalCandleRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DhanMarketDataService {

    @Autowired
    private WebClient dhanWebClient;

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
                .onStatus(HttpStatusCode::isError, r ->
                r.bodyToMono(String.class)
                 .flatMap(body -> {
                     log.error("DHAN ERROR => {}", body);
                     return Mono.error(new RuntimeException(body));
                 })
            )
                .bodyToMono(Candle.class);
    }

	public List<Candle> fetchIntradayCandles(List<HistoricalCandleRequest> request) {
		List<Candle> listRsiCandle = new ArrayList<Candle>();
		request.forEach(r->{
			@Nullable
			Candle candle = fetchIntradayCandles(r).block();
			listRsiCandle.add(candle);
		});
		return listRsiCandle;
	}
	
	public String downloadInstrumentFile() {
        return dhanWebClient.get()
                .uri("/v2/instrument/NSE_FNO")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
