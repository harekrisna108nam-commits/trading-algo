package com.example.dhan_rsi_series.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DhanOrderRequest {

    private String dhanClientId;
    private String correlationId;
    private String transactionType;
    private String exchangeSegment;
    private String productType;
    private String orderType;
    private String validity;
    private String securityId;
    private int quantity;
    private int disclosedQuantity;
    private double price;
    private double triggerPrice;
    private boolean afterMarketOrder;
    private String amoTime;
}
