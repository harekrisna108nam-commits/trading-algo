package com.example.dhan_rsi_series.model;

import lombok.Data;

@Data
public class Position {

    private String dhanClientId;
    private String tradingSymbol;
    private String securityId;

    private String positionType;   // LONG / SHORT
    private String exchangeSegment;
    private String productType;

    private double buyAvg;
    private int buyQty;

    private double sellAvg;
    private int sellQty;

    private int netQty;

    private double realizedProfit;
    private double unrealizedProfit;

    private String drvOptionType;     // CALL / PUT (IMPORTANT)
    private double drvStrikePrice;    // strike
}