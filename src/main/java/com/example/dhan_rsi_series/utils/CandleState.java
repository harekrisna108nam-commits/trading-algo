package com.example.dhan_rsi_series.utils;

public class CandleState {

    private final int timeframeSeconds;
    private long startEpoch;
    private long nowEpoc;

    private double open;
    private double high;
    private double low;
    private double close;

    private boolean started = false;

    public CandleState(int timeframeSeconds) {
        this.timeframeSeconds = timeframeSeconds;
    }

    public void onTick(double price, long nowEpoch) {

        if (!started) {
            startEpoch = nowEpoch;
            open = high = low = close = price;
            started = true;
            return;
        }

        high = Math.max(high, price);
        low  = Math.min(low, price);
        close = price;
    }

    public boolean isComplete(long nowEpoch) {
    	nowEpoc = nowEpoch;
        return started && (nowEpoch - startEpoch) >= timeframeSeconds;
    }

    public CandleSnapshot snapshotAndReset() {

        CandleSnapshot snap = new CandleSnapshot(
                open, high, low, close, nowEpoc
        );

        started = false;
        return snap;
    }
    }
