package com.example.dhan_rsi_series.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class RsiCandle extends CandleData {
    private double rsi;       // RSI value
    private double vwap;      // VWAP value
    private double highRsi;
    private double lowRsi;
    private double deltaRsi;
    private double callFlow;
    private double putFlow;
    private double netFlow;
    private double highestOi;
    private double atp;
    private int securityId;
}

