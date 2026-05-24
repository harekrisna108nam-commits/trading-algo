package com.example.dhan_rsi_series.model;

import java.time.LocalDate;

public record Tick(
        int securityId,
        double ltp,
        int oi,
        int highestOi,
        double atp,
        String optionType,
        LocalDate expiry
) {}