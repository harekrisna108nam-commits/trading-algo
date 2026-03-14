package com.example.dhan_rsi_series.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dhan_rsi_series.model.SubscriptionRequest;
import com.example.dhan_rsi_series.utils.DhanLiveDataHandler;
import com.example.dhan_rsi_series.utils.DhanSubscriptionStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/subscriptions")
public class DhanSubscriptionController {

    private final DhanSubscriptionStore store;
    private final DhanLiveDataHandler handler;

    public DhanSubscriptionController(
            DhanSubscriptionStore store,
            DhanLiveDataHandler handler) {

        this.store = store;
        this.handler = handler;
    }

    /**
     * Persist subscription + auto subscribe if WS is connected
     */
    @PostMapping
    public Mono<ResponseEntity<String>> subscribe(@RequestBody SubscriptionRequest req) {

        if (req.getExchange() == null ||
            req.getSecurityIds() == null ||
            req.getOptionType() == null) {

            return Mono.just(
                ResponseEntity.badRequest()
                    .body("exchange, securityId, optionType are required")
            );
        }

        return Flux.fromIterable(req.getSecurityIds())

            // 1️⃣ persist
            .doOnNext(securityId ->
                store.add(req.getExchange(), securityId, req.getOptionType())
            )

            // 2️⃣ subscribe socket (async)
            .flatMap(securityId ->
                handler.subscribe(
                    req.getExchange(),
                    securityId,
                    req.getOptionType()
                )
            )

            // wait for all
            .then(
                Mono.just(
                    ResponseEntity.ok(
                        "Subscribed %d securities"
                            .formatted(req.getSecurityIds().size())
                    )
                )
            );
    }
    
}
