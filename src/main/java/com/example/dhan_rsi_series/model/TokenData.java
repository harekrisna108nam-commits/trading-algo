package com.example.dhan_rsi_series.model;

import java.time.Instant;

public class TokenData {

    private final String token;
    private final Instant expiry;

    public TokenData(String token, Instant expiry) {
        this.token = token;
        this.expiry = expiry;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiry() {
        return expiry;
    }
}