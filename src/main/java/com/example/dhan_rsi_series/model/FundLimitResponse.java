package com.example.dhan_rsi_series.model;

import lombok.Data;

@Data
public class FundLimitResponse {

    private String dhanClientId;
    private double availabelBalance;
    private double sodLimit;
    private double collateralAmount;
    private double receiveableAmount;
    private double utilizedAmount;
    private double blockedPayoutAmount;
    private double withdrawableBalance;
}