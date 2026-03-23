package com.example.dhan_rsi_series.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.dhan_rsi_series.service.DhanAuthService;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Value("${dhan.base-url}")
    private String baseUrl;

    @Value("${dhan.access-token}")
    private String accessToken;
    
    @Value("${dhan.client-id}")
    private String clientId;
    
    private final DhanAuthService authService;

    public WebClientConfig(DhanAuthService authService) {
        this.authService = authService;
    }
    
//    @Bean
//    public WebClient dhanWebClient() {
//        return WebClient.builder()
//                .baseUrl(baseUrl)
//                .defaultHeader("access-token", accessToken)
//                .build();
//    }
    
//    @Bean
//    public WebClient dhanWebClient() {
//
//        return WebClient.builder()
//            .baseUrl(baseUrl)
//            .filter(authFilter())
//            .filter((req, next) -> {
//                System.out.println("HEADERS => " + req.headers());
//                return next.exchange(req);
//            })
//            .build();
//    }
    
    @Bean
    public WebClient dhanWebClient() {

        HttpClient httpClient = HttpClient.create()
                .followRedirect(true); // ✅ CRITICAL FIX

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(20 * 1024 * 1024)) // ✅ 20MB buffer (CSV is ~15MB)
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // ✅ attach client
                .exchangeStrategies(strategies) // ✅ increase buffer
                .filter(authFilter())
                .filter((req, next) -> {
                    System.out.println("HEADERS => " + req.headers());
                    return next.exchange(req);
                })
                .build();
    }
    
    // 🔐 Inject token dynamically
    private ExchangeFilterFunction authFilter() {

        return (request, next) ->
            authService.getAccessToken()
                .flatMap(token -> {

                    ClientRequest newReq =
                        ClientRequest.from(request)
                         .header("access-token", token.trim())
                        //.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                            .build();

                    return next.exchange(newReq);
                });
    }
}
