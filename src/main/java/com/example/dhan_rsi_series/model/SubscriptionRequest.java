package com.example.dhan_rsi_series.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class SubscriptionRequest {
    private String exchange;
    //private String securityId;
    private List<String> securityIds;
    private Map<String, Integer> map;
    private String optionType; // CALL / PUT
    //private int strike;
}
