package com.example.dhan_rsi_series.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionRsiExcelRequest {
    private String timeframe;   // 1M / 5S
    private String optionType;  // CALL / PUT
}
