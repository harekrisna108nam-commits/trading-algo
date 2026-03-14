package com.example.dhan_rsi_series.utils;

public class WilderRsiCalculator {

    private final int period;

    private double avgGain = 0;
    private double avgLoss = 0;

    private Double prevClose = null;
    private int count = 0;

    public WilderRsiCalculator(int period) {
        this.period = period;
    }

    /**
     * Call once per candle close
     * Returns:
     *   -1  -> not enough data yet
     *   0-100 -> RSI value
     */
    public double update(double close) {

        // first candle
        if (prevClose == null) {
            prevClose = close;
            return -1;
        }

        double diff = close - prevClose;
        prevClose = close;

        double gain = Math.max(diff, 0);
        double loss = Math.max(-diff, 0);

        count++;

        // ========= SEED PHASE =========
        if (count <= period) {
            avgGain += gain;
            avgLoss += loss;

            if (count == period) {
                avgGain /= period;
                avgLoss /= period;
            }

            return -1;
        }

        // ========= WILDER SMOOTHING =========
        avgGain = ((avgGain * (period - 1)) + gain) / period;
        avgLoss = ((avgLoss * (period - 1)) + loss) / period;

        if (avgLoss == 0) return 100;

        double rs = avgGain / avgLoss;

        return 100 - (100 / (1 + rs));
    }
}

