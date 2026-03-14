package com.example.dhan_rsi_series.model;

public record Tick(
        int securityId,
        double ltp,
        int oi,
        int highestOi,
        double atp,
        String optionType
) {}