package com.example.dhan_rsi_series.utils;

import java.util.List;

/*public class LiveRsiCalculator {

    public static double calculate(List<Double> closes, int period) {

        if (closes.size() < period + 1) {
            return -1;
        }

        double gain = 0;
        double loss = 0;

        for (int i = closes.size() - period; i < closes.size(); i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            if (diff > 0) gain += diff;
            else loss -= diff;
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        if (avgLoss == 0) return 100;

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}*/

public class LiveRsiCalculator {

    private final int period;

    private double avgGain = 0;
    private double avgLoss = 0;

    private Double prevClose = null;
    private int count = 0;

    public LiveRsiCalculator(int period) {
        this.period = period;
    }

    public double update(double close) {

        if (prevClose == null) {
            prevClose = close;
            return -1;
        }

        double diff = close - prevClose;
        prevClose = close;

        double gain = Math.max(diff, 0);
        double loss = Math.max(-diff, 0);

        count++;

        if (count <= period) {
            avgGain += gain;
            avgLoss += loss;

            if (count == period) {
                avgGain /= period;
                avgLoss /= period;
            }
            return -1;
        }

        avgGain = ((avgGain * (period - 1)) + gain) / period;
        avgLoss = ((avgLoss * (period - 1)) + loss) / period;

        if (avgLoss == 0) return 100;

        double rs = avgGain / avgLoss;

        return 100 - (100 / (1 + rs));
    }
}
