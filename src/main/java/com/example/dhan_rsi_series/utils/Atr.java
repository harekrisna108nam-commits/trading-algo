package com.example.dhan_rsi_series.utils;

import com.example.dhan_rsi_series.model.CandleData;

public class Atr {

    private final int period;
    private double atr = 0;
    private Double prevClose = null;
    private int count = 0;

    public Atr(int period) {
        this.period = period;
    }

    public double update(CandleData c) {

        double tr;

        if (prevClose == null) {
            tr = c.getHigh() - c.getLow();
        } else {
            tr = Math.max(
                    c.getHigh() - c.getLow(),
                    Math.max(
                        Math.abs(c.getHigh() - prevClose),
                        Math.abs(c.getLow() - prevClose)
                    )
            );
        }

        prevClose = c.getClose();

        count++;

        if (count <= period) {
            atr += tr;
            if (count == period) atr /= period;
            return -1;
        }

        atr = ((atr * (period - 1)) + tr) / period;

        return atr;
    }
}
