package com.example.dhan_rsi_series.utils;

public class EmaSmoothedRsiCalculator {

    // ========= CONFIG =========
    private final int rsiPeriod;
    private final int smoothPeriod;

    // ========= RSI STATE =========
    private double avgGain = 0;
    private double avgLoss = 0;
    private Double prevClose = null;
    private int count = 0;

    // ========= EMA STATE =========
    private Double emaRsi = null;
    private final double alpha;

    public EmaSmoothedRsiCalculator(int rsiPeriod, int smoothPeriod) {
        this.rsiPeriod = rsiPeriod;
        this.smoothPeriod = smoothPeriod;

        // EMA multiplier
        this.alpha = 2.0 / (smoothPeriod + 1);
    }

    public double update(double close) {

        // ==============================
        // Step 1 → Wilder RSI
        // ==============================

        if (prevClose == null) {
            prevClose = close;
            return -1;
        }

        double diff = close - prevClose;
        prevClose = close;

        double gain = Math.max(diff, 0);
        double loss = Math.max(-diff, 0);

        count++;

        // initial seed
        if (count <= rsiPeriod) {
            avgGain += gain;
            avgLoss += loss;

            if (count == rsiPeriod) {
                avgGain /= rsiPeriod;
                avgLoss /= rsiPeriod;
            }
            return -1;
        }

        avgGain = ((avgGain * (rsiPeriod - 1)) + gain) / rsiPeriod;
        avgLoss = ((avgLoss * (rsiPeriod - 1)) + loss) / rsiPeriod;

        double rsi;

        if (avgLoss == 0) rsi = 100;
        else {
            double rs = avgGain / avgLoss;
            rsi = 100 - (100 / (1 + rs));
        }

        // ==============================
        // Step 2 → EMA smoothing
        // ==============================

        if (emaRsi == null) {
            emaRsi = rsi; // seed
            return -1;
        }

        emaRsi = (rsi - emaRsi) * alpha + emaRsi;

        return emaRsi;
    }
}

