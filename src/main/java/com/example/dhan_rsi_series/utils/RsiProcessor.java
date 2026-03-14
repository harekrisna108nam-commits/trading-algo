package com.example.dhan_rsi_series.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import com.example.dhan_rsi_series.entity.RsiEntity;
import com.example.dhan_rsi_series.repository.RsiRepository;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class RsiProcessor {

    private static final int RSI_PERIOD = 14;
    private static final long CANDLE_MS = 5_000;

    private Candle candle;
    private long candleStart = -1;

    private final Deque<Double> closes = new ArrayDeque<>();
    private Double avgGain, avgLoss;

    private final String instrumentId;
    private final String optionType;
    private final RsiRepository repo;

    public RsiProcessor(String instrumentId, String optionType, RsiRepository repo) {
        this.instrumentId = instrumentId;
        this.optionType = optionType;
        this.repo = repo;
    }

    public void onTick(double ltp) {

        long now = System.currentTimeMillis();

        if (candleStart == -1) {
            startCandle(now, ltp);
            return;
        }

        if (now - candleStart < CANDLE_MS) {
            candle.update(ltp);
        } else {
            closeCandle();
            startCandle(now, ltp);
        }
    }

    private void startCandle(long time, double price) {
        candleStart = time;
        candle = new Candle(price);
    }

    private void closeCandle() {

        double close = candle.close;
        closes.addLast(close);

        if (closes.size() > RSI_PERIOD) {
            closes.removeFirst();
        }

        if (closes.size() == RSI_PERIOD) {
            double rsi = calculateRsi(close);
            saveRsi(close, rsi);
        }
    }

    private double calculateRsi(double latest) {

        if (avgGain == null) {
            initRsi();
        } else {
            double prev = getPrevClose();
            double diff = latest - prev;

            avgGain = (avgGain * (RSI_PERIOD - 1) + Math.max(diff, 0)) / RSI_PERIOD;
            avgLoss = (avgLoss * (RSI_PERIOD - 1) + Math.max(-diff, 0)) / RSI_PERIOD;
        }

        if (avgLoss == 0) return 100;
        return 100 - (100 / (1 + avgGain / avgLoss));
    }

    private void initRsi() {

        Iterator<Double> it = closes.iterator();
        double prev = it.next();

        double gain = 0, loss = 0;

        while (it.hasNext()) {
            double curr = it.next();
            double diff = curr - prev;

            if (diff > 0) gain += diff;
            else loss -= diff;

            prev = curr;
        }

        avgGain = gain / RSI_PERIOD;
        avgLoss = loss / RSI_PERIOD;
    }

    private double getPrevClose() {
        Iterator<Double> it = closes.descendingIterator();
        it.next();
        return it.next();
    }

    private void saveRsi(double close, double rsi) {

        RsiEntity e = new RsiEntity();
        e.setInstrumentId(instrumentId);
        e.setOptionType(optionType);
        e.setTimeframe("5s");
        e.setCandleCloseTime(System.currentTimeMillis());
        e.setClosePrice(close);
        e.setRsiPeriod(RSI_PERIOD);
        e.setRsiValue(rsi);

        Mono.fromRunnable(() -> repo.save(e))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();

        System.out.println(optionType + " RSI → " + rsi);
    }

    static class Candle {
        double open, high, low, close;
        Candle(double p) { open = high = low = close = p; }
        void update(double p) {
            close = p;
            high = Math.max(high, p);
            low = Math.min(low, p);
        }
    }
}