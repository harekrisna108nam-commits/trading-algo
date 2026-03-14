package com.example.dhan_rsi_series.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.utils.DpiAggregatorService.Bucket;

/*@Component
public class DpiAggregatorService {

    // timeframe -> bucket
    private final ConcurrentHashMap<Integer, Bucket> buckets =
            new ConcurrentHashMap<>();

    // =====================================================
    // ADD (called for every OptionRsi tick)
    // =====================================================
    public void add(OptionRsi r, int timeframe) {

        Bucket b = buckets.computeIfAbsent(timeframe, tf -> new Bucket());

        if ("CE".equals(r.getOptionType())) {
            b.call.add(r.getDpi());
        } else {
            b.put.add(r.getDpi());
        }
    }

    // =====================================================
    // READ
    // =====================================================
    public double call(int tf) {
        return bucket(tf).call.sum();
    }

    public double put(int tf) {
        return bucket(tf).put.sum();
    }

    public double net(int tf) {
        Bucket b = bucket(tf);
        return b.call.sum() - b.put.sum();
    }

    // =====================================================
    // SNAPSHOT (recommended for trading)
    // =====================================================
    public Snapshot snapshot(int tf) {
        Bucket b = bucket(tf);

        double call = b.call.sum();
        double put  = b.put.sum();

        return new Snapshot(call, put);
    }

    // =====================================================
    // RESET (new candle start)
    // =====================================================
    public void reset(int tf) {
        buckets.put(tf, new Bucket());
    }

    // =====================================================
    // INTERNAL
    // =====================================================
    private Bucket bucket(int tf) {
        return buckets.computeIfAbsent(tf, k -> new Bucket());
    }

    // =====================================================
    // BUCKET
    // =====================================================
    private static class Bucket {
        final DoubleAdder call = new DoubleAdder();
        final DoubleAdder put  = new DoubleAdder();
    }

    // =====================================================
    // DTO
    // =====================================================
    public record Snapshot(double call, double put) {
        public double net() {
            return call - put;
        }
    }
}*/

@Service
public class DpiAggregatorService {

    private final Map<Integer, Bucket> buckets = new ConcurrentHashMap<>();

    // =========================================================

    public void add(OptionRsi r, int tf) {

        Bucket b = buckets.computeIfAbsent(tf, t -> new Bucket());

        if ("CALL".equals(r.getOptionType()))
            b.call.add(r.getDpi());
        else
            b.put.add(r.getDpi());
    }

    // =========================================================

    public Snapshot snapshot(int tf) {

        Bucket b = buckets.get(tf);
        if (b == null) return new Snapshot(0,0);

        return new Snapshot(
                b.call.sum(),
                b.put.sum()
        );
    }

    public void reset(int tf) {
        buckets.put(tf, new Bucket());
    }

    // =========================================================

    static class Bucket {
        DoubleAdder call = new DoubleAdder();
        DoubleAdder put  = new DoubleAdder();
    }

    // =========================================================

    public record Snapshot(double call, double put) {
        public double net() { return call - put; }
    }
}