package com.example.dhan_rsi_series.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalCandleRequest {

    @JsonProperty("securityId")
    private String securityId;

    @JsonProperty("exchangeSegment")
    private String exchangeSegment;

    @JsonProperty("instrument")
    private String instrument;

    @JsonProperty("interval")
    private String interval;

    @JsonProperty("oi")
    private boolean oi;

    @JsonProperty("fromDate")
    private String fromDate;

    @JsonProperty("toDate")
    private String toDate;
}

