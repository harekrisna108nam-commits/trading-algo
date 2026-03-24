package com.example.dhan_rsi_series.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.dhan_rsi_series.model.TokenData;

import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/*@Service
public class DhanAuthService {

    @Value("${dhan.client-id}")
    private String clientId;

    @Value("${dhan.api-key}")
    private String apiKey;

    private final WebClient webClient;

    private String cachedToken;
    private Instant expiryTime;

    public DhanAuthService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://api.dhan.co")
                .build();
    }

    // 🔑 PUBLIC METHOD
    public Mono<String> getAccessToken() {

        if (cachedToken != null &&
            expiryTime != null &&
            Instant.now().isBefore(expiryTime.minusSeconds(60))) {

            return Mono.just(cachedToken);
        }

        return generateToken();
    }

    // 🔐 ACTUAL TOKEN GENERATION
    private Mono<String> generateToken() {

        DhanTokenRequest req = new DhanTokenRequest();
        req.setClientId(clientId);
        req.setApiKey(apiKey);

        return webClient.post()
            .uri("/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .bodyToMono(DhanTokenResponse.class)
            .map(resp -> {
                cachedToken = resp.getAccessToken();
                expiryTime = Instant.now()
                        .plusSeconds(resp.getExpiresIn());
                return cachedToken;
            });
    }
}*/

/*@Service
public class DhanAuthService {

    @Value("${dhan.client-id}")
    private String clientId;

    @Value("${dhan.api-key}")
    private String apiKey;
    
    @Value("${dhan.api-secret}")
    private String apiSecret;

    private final WebClient webClient;

    private volatile String cachedToken;
    private volatile Instant expiry;

    public DhanAuthService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://api.dhan.co")
                .build();
    }
    
    curl --location --request POST 'https://auth.dhan.co/app/generate-consent?client_id={dhanClientId}' \
    --header 'app_id: {API key}' \
    --header 'app_secret: {API secret}'


    public Mono<String> getAccessToken() {

        if (cachedToken != null &&
            expiry != null &&
            Instant.now().isBefore(expiry.minusSeconds(60))) {

            return Mono.just(cachedToken);
        }

        return webClient.post()
            .uri("/app/generate-consent")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "clientId", clientId,
                "apiKey", apiKey
            ))
            .retrieve()
            .bodyToMono(Map.class)
            .map(resp -> {
                cachedToken = (String) resp.get("accessToken");
                int expiresIn = (int) resp.get("expiresIn");
                expiry = Instant.now().plusSeconds(expiresIn);
                return cachedToken;
            });
    }
}*/

@Service
public class DhanAuthService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile String token;
    private volatile Instant expiry;
    @Value("${dhan.api-key}")
    private String apiKey;
    
    @Value("${dhan.api-secret}")
    private String apiSecret;
    
    @Value("${dhan.client-id}")
    private String clientId;

    public DhanAuthService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }
    
//    curl --location 'https://auth.dhan.co/app/consumeApp-consent?tokenId={Token ID}' \
//    --header 'app_id: {API Key}' \
//    --header 'app_secret: {API Secret}'

    
    // 1️⃣ Generate Consent JWT
    public Mono<String> generateConsent(String clientId, String apiKey, String apiSecret) {
        String url = "https://auth.dhan.co/app/consumeApp-consent?tokenId=" + "ee235fbe-8635-4679-afb8-6c3e72225f4a";

//        return webClient.post()
//                .uri(url)
//                .header("app_id", apiKey)
//                .header("app_secret", apiSecret)
//                .retrieve()
//                .bodyToMono(String.class)
//                .map(resp -> {
//                    try {
//                        JsonNode node = objectMapper.readTree(resp);
//                        return node.get("accessToken").asText().trim(); // jwtToken key from Dhan response
//                    } catch (Exception e) {
//                        throw new RuntimeException("Failed to parse JWT token from GenerateConsent", e);
//                    }
//                });
        
//        return webClient.post()
//                .uri(url)
//                .header("app_id", apiKey)
//                .header("app_secret", apiSecret)
//                .retrieve()
//                .bodyToMono(JsonNode.class)
//                .map(node -> {
//
//                    String accessToken = node.get("accessToken").asText();
//
//                    LocalDateTime ldt = LocalDateTime.parse(node.get("expiryTime").asText());
//                    Instant expiry = ldt.atZone(ZoneId.systemDefault()).toInstant();
//
//                    return new TokenData(accessToken, expiry);
//                });
        
        return Mono.just("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzc0MzY4Mzk3LCJpYXQiOjE3NzQyODE5OTcsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTA2Mjg5ODAzIn0.JebGe4poeKngDImOy2bcaFU75fR0OYLSxOgJoWB0jfB4OX7ye4zoWBp7xk-EWzomibbu14D4mcS0qYpVAGFMwA");
    }
    
 // 2️⃣ Renew Token
    public Mono<String> renewToken(String jwtToken, String clientId) {
        String url = "https://api.dhan.co/v2/RenewToken";

        return webClient.post()
                .uri(url)
                .header("access-token", jwtToken.trim())
                .header("dhanClientId", clientId.trim())
                .retrieve()
                .bodyToMono(String.class)
                .map(resp -> {
                    try {
                        JsonNode node = objectMapper.readTree(resp);
                        return node.get("accessToken").asText(); // new token key
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse access token from RenewToken", e);
                    }
                });
    }

//    public Mono<TokenData> getAccessToken() {
//    	return generateConsent(clientId, apiKey, apiSecret);
//    }
    
    public Mono<String> getAccessToken() {

        if (token != null && expiry != null && Instant.now().isBefore(expiry.minusSeconds(60))) {
            return Mono.just(token);
        }

        return generateConsent(clientId, apiKey, apiSecret);
//                .map(t -> {
//                    this.token = t.getToken();
//                    this.expiry = t.getExpiry();
//                    return token;
//                });
    }

    private String extractAccessToken(String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            return node.get("accessToken").asText();          // adjust key if different
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
    }
}
