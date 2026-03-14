package com.example.dhan_rsi_series.model;

import lombok.Data;

@Data
public class DhanTokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
}
