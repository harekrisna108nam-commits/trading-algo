package com.example.dhan_rsi_series.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.dhan_rsi_series.model.DhanOrderRequest;
import com.example.dhan_rsi_series.model.DhanOrderResponse;
import com.example.dhan_rsi_series.model.Position;

import reactor.core.publisher.Mono;

@Service
public class DhanOrderService {

    @Autowired
    private WebClient dhanWebClient;

    public Mono<DhanOrderResponse> placeOrder(String accessToken, DhanOrderRequest request) {

        return dhanWebClient.post()
                .uri("/v2/orders")
                .header("Content-Type", "application/json")
                //.header("access-token", accessToken)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.out.println("❌ DHAN API ERROR BODY: " + errorBody);
                                    return Mono.error(new RuntimeException(errorBody));
                                })
                )
                .bodyToMono(DhanOrderResponse.class);
    }
    

    public Mono<List<Position>> getPositions(String accessToken) {

        return dhanWebClient.get()
                .uri("/v2/positions")
                .header("Content-Type", "application/json")
                //.header("access-token", accessToken)
                .retrieve()
                .bodyToFlux(Position.class)   // 👈 IMPORTANT (array response)
                .collectList()
                .doOnSuccess(list -> System.out.println("✅ Positions fetched: " + list.size()))
                .doOnError(err -> System.out.println("❌ Error fetching positions: " + err.getMessage()));
    }
}