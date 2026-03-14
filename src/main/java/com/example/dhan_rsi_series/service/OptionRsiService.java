package com.example.dhan_rsi_series.service;

import java.util.List;

import com.example.dhan_rsi_series.model.RsiCandle;

public interface OptionRsiService {
    List<RsiCandle> getRsiCandles(String timeframe, String optionType);
}
