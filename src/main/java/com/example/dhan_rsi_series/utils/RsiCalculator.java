package com.example.dhan_rsi_series.utils;

import java.util.List;

public class RsiCalculator {

    public static double calculate(List<Double> closes, int period) {

        if (closes.size() < period + 1) return -1;

        double gain = 0;
        double loss = 0;

        for (int i = closes.size() - period; i < closes.size(); i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            if (diff >= 0) gain += diff;
            else loss -= diff;
        }

        if (loss == 0) return 100;

        double rs = gain / loss;
        return 100 - (100 / (1 + rs));
    }
}
