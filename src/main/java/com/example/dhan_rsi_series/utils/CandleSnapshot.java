package com.example.dhan_rsi_series.utils;

public record CandleSnapshot(
        double open,
        double high,
        double low,
        double close,
        long startEpoch,
        double avgLtp
) {}
