package com.example.dhan_rsi_series.model;

import java.util.List;

import lombok.Data;

@Data
public class Candle {
	private List<Double> open;
    private List<Double> high;
    private List<Double> low;
    private List<Double> close;
    private List<Double> volume;
    private List<Long> timestamp;
    private List<Double> open_interest;
}

