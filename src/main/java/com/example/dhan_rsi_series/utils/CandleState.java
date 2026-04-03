package com.example.dhan_rsi_series.utils;

import lombok.Getter;
import lombok.Setter;

//public class CandleState {
//
//    private final int timeframeSeconds;
//    private long startEpoch;
//    private long nowEpoc;
//
//    private double open;
//    private double high;
//    private double low;
//    private double close;
//
//    private boolean started = false;
//
//    public CandleState(int timeframeSeconds) {
//        this.timeframeSeconds = timeframeSeconds;
//    }
//
//    public void onTick(double price, long nowEpoch) {
//
//        if (!started) {
//            startEpoch = nowEpoch;
//            open = high = low = close = price;
//            started = true;
//            return;
//        }
//
//        high = Math.max(high, price);
//        low  = Math.min(low, price);
//        close = price;
//    }
//
//    public boolean isComplete(long nowEpoch) {
//    	nowEpoc = nowEpoch;
//        return started && (nowEpoch - startEpoch) >= timeframeSeconds;
//    }
//
//    public CandleSnapshot snapshotAndReset() {
//
//        CandleSnapshot snap = new CandleSnapshot(
//                open, high, low, close, nowEpoc
//        );
//
//        started = false;
//        return snap;
//    }
//    }

@Getter
@Setter
public class CandleState {

    private final int timeframeSeconds;

    private long startEpoch;

    private double open;
    private double high;
    private double low;
    private double close;

    private boolean started = false;

    public CandleState(int timeframeSeconds) {
        this.timeframeSeconds = timeframeSeconds;
    }

    public void onTick(double price, long nowEpoch) {

        // start new candle
        if (!started) {
            startEpoch = nowEpoch;
            open = high = low = close = price;
            started = true;
            return;
        }

        // update current candle
        high = Math.max(high, price);
        low = Math.min(low, price);
        close = price;
    }

    public boolean isComplete(long nowEpoch) {
        return started && (nowEpoch - startEpoch) >= timeframeSeconds;
    }

    // ✅ CONTINUOUS CANDLE (NO GAP FIX)
    public CandleSnapshot snapshotAndReset(long nowEpoch, double lastPrice) {

        CandleSnapshot snap = new CandleSnapshot(
                open, high, low, close, startEpoch   // ✅ FIXED TIME
        );

        // ✅ Immediately start next candle (CRITICAL FIX)
        startEpoch = nowEpoch;
        open = high = low = close = lastPrice;
        started = true;

        return snap;
    }
}
