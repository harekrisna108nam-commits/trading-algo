package com.example.dhan_rsi_series.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.dhan_rsi_series.service.DhanAuthService;

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
    
    @Bean
    public WebClient dhanWebClient() {

        return WebClient.builder()
            .baseUrl(baseUrl)
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
