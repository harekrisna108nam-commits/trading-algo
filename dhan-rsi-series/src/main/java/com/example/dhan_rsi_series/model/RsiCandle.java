package com.example.dhan_rsi_series.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class RsiCandle extends CandleData {
    private double rsi;       // RSI value
    private double vwap;      // VWAP value
}

