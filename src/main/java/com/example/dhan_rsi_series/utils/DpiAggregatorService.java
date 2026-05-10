package com.example.dhan_rsi_series.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.entity.DhanSubscription;
import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.repository.OptionRsiRepository;

import jakarta.annotation.PostConstruct;

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

//@Service
//public class DpiAggregatorService {
//
//    private final Map<Integer, Bucket> buckets = new ConcurrentHashMap<>();
//
//    // =========================================================
//
//    public void add(OptionRsi r, int tf) {
//
//        Bucket b = buckets.computeIfAbsent(tf, t -> new Bucket());
//
//        if ("CALL".equals(r.getOptionType()))
//            b.call.add(r.getDpi());
//        else
//            b.put.add(r.getDpi());
//    }
//
//    // =========================================================
//
//    public Snapshot snapshot(int tf) {
//
//        Bucket b = buckets.get(tf);
//        if (b == null) return new Snapshot(0,0);
//
//        return new Snapshot(
//                b.call.sum(),
//                b.put.sum()
//        );
//    }
//
//    public void reset(int tf) {
//        buckets.put(tf, new Bucket());
//    }
//
//    // =========================================================
//
//    static class Bucket {
//        DoubleAdder call = new DoubleAdder();
//        DoubleAdder put  = new DoubleAdder();
//    }
//
//    // =========================================================
//
//    public record Snapshot(double call, double put) {
//        public double net() { return call - put; }
//    }
//}

//@Service
//public class DpiAggregatorService {
//
//    private final Map<Integer, Bucket> buckets = new ConcurrentHashMap<>();
//
//    // =========================================================
//
//    public void add(OptionRsi r, int tf) {
//
//        Bucket b = buckets.computeIfAbsent(tf, t -> new Bucket());
//
//        if ("CALL".equals(r.getOptionType())) {
//            b.callDpi.add(r.getDpi());
//            b.callDeltaRsi.add(r.getDeltaRsi());
//        } else {
//            b.putDpi.add(r.getDpi());
//            b.putDeltaRsi.add(r.getDeltaRsi());
//        }
//    }
//
//    // =========================================================
//
//    public Snapshot snapshot(int tf) {
//
//        Bucket b = buckets.get(tf);
//        if (b == null) return new Snapshot(0, 0, 0, 0);
//
//        return new Snapshot(
//                b.callDpi.sum(),
//                b.putDpi.sum(),
//                b.callDeltaRsi.sum(),
//                b.putDeltaRsi.sum()
//        );
//    }
//
//    public void reset(int tf) {
//        buckets.put(tf, new Bucket());
//    }
//
//    // =========================================================
//
//    static class Bucket {
//
//        // DPI
//        DoubleAdder callDpi = new DoubleAdder();
//        DoubleAdder putDpi  = new DoubleAdder();
//
//        // Delta RSI
//        DoubleAdder callDeltaRsi = new DoubleAdder();
//        DoubleAdder putDeltaRsi  = new DoubleAdder();
//    }
//
//    // =========================================================
//
//    public record Snapshot(
//            double callDpi,
//            double putDpi,
//            double callDeltaRsi,
//            double putDeltaRsi
//    ) {
//
//        public double netDpi() {
//            return callDpi - putDpi;
//        }
//
//        public double netDeltaRsi() {
//            return callDeltaRsi - putDeltaRsi;
//        }
//    }
//}


//@Service
//public class DpiAggregatorService {
//
//    private final Map<Integer, Bucket> buckets = new ConcurrentHashMap<>();
//    private final OptionRsiRepository optionRsiRepository;
//    
//    public DpiAggregatorService(OptionRsiRepository optionRsiRepository) {
//		super();
//		this.optionRsiRepository = optionRsiRepository;
//	}
//
//	// =========================================================
//    // 🔥 LOAD LAST CALLFLOW AND PUTFLOW FROM DB
//    // =========================================================
//    @PostConstruct
//    public void loadMoneyFlow() {
//    	// here load last record from db and use it as callFlow and PutFlow
//        
//    }
//
//    // =========================================================
//    // ADD DATA
//    // =========================================================
//    public void add(OptionRsi r, int tf) {
//
//        Bucket b = buckets.computeIfAbsent(tf, t -> new Bucket());
//
//        if ("CALL".equalsIgnoreCase(r.getOptionType())) {
//
//            b.callDpi.add(r.getDpi());
//            b.callWeightedOi.add(r.getWeightedOi());
//
//        } else {
//
//            b.putDpi.add(r.getDpi());
//            b.putWeightedOi.add(r.getWeightedOi());
//        }
//    }
//
//    // =========================================================
//    // SNAPSHOT
//    // =========================================================
//    public Snapshot snapshot(int tf) {
//
//        Bucket b = buckets.get(tf);
//        if (b == null) return new Snapshot(0, 0, 0, 0, 0);
//
//        return new Snapshot(
//                b.callDpi.sum(),
//                b.putDpi.sum(),
//                b.callWeightedOi.sum(),
//                b.putWeightedOi.sum(),
//                b.callWeightedOi.sum() - b.putWeightedOi.sum()
//        );
//    }
//
//    // =========================================================
//    // RESET
//    // =========================================================
//    public void reset(int tf) {
//        buckets.put(tf, new Bucket());
//    }
//
//    // =========================================================
//    // INTERNAL BUCKET
//    // =========================================================
//    static class Bucket {
//
//        // FLOW
//        DoubleAdder callDpi = new DoubleAdder();
//        DoubleAdder putDpi  = new DoubleAdder();
//
//        // WEIGHTED OI
//        DoubleAdder callWeightedOi = new DoubleAdder();
//        DoubleAdder putWeightedOi  = new DoubleAdder();
//    }
//
//    // =========================================================
//    // SNAPSHOT MODEL
//    // =========================================================
//    public record Snapshot(
//            double callDpi,
//            double putDpi,
//            double callWeightedOi,
//            double putWeightedOi,
//            double weightedOi   // (call - put)
//    ) {
//
//        public double netDpi() {
//            return callDpi - putDpi;
//        }
//    }
//}

@Service
public class DpiAggregatorService {

    private final Map<Integer, Bucket> buckets = new ConcurrentHashMap<>();
    private final OptionRsiRepository optionRsiRepository;

    public DpiAggregatorService(OptionRsiRepository optionRsiRepository) {
        this.optionRsiRepository = optionRsiRepository;
    }

    // =========================================================
    // 🔥 LOAD LAST FLOW FROM DB (ON STARTUP)
    // =========================================================
    @PostConstruct
    public void loadMoneyFlow() {

        try {
            OptionRsi last = optionRsiRepository.findLatestByTimeframe("5S");

            Bucket bucket = buckets.computeIfAbsent(5, t -> new Bucket());

            if (last == null) {

                // ✅ CLEAN START
                bucket.baseCallFlow = 0;
                bucket.basePutFlow  = 0;

                System.out.println("🟡 No DB data → Starting fresh from 0");
                return;
            }

            // ✅ RESTORE STATE
            bucket.baseCallFlow = safe(last.getCallFlow());
            bucket.basePutFlow  = safe(last.getPutFlow());

            bucket.callWeightedOi.add(safe(last.getCallWeightedOi()));
            bucket.putWeightedOi.add(safe(last.getPutWeightedOi()));

            System.out.println("✅ MoneyFlow restored from DB");
            System.out.println("CALL FLOW: " + bucket.baseCallFlow);
            System.out.println("PUT FLOW : " + bucket.basePutFlow);

        } catch (Exception e) {

            // ✅ FAIL-SAFE (NEVER BREAK SYSTEM)
            Bucket bucket = buckets.computeIfAbsent(5, t -> new Bucket());

            bucket.baseCallFlow = 0;
            bucket.basePutFlow  = 0;

            System.out.println("❌ DB load failed → fallback to ZERO state");
            e.printStackTrace();
        }
    }

    // =========================================================
    // ADD DATA (REAL-TIME)
    // =========================================================
    public void add(OptionRsi r, int tf) {

        Bucket b = buckets.computeIfAbsent(tf, t -> new Bucket());

        if ("CALL".equalsIgnoreCase(r.getOptionType())) {

            b.callDpi.add(safe(r.getDpi()));
            b.callWeightedOi.add(safe(r.getWeightedOi()));

        } else {

            b.putDpi.add(safe(r.getDpi()));
            b.putWeightedOi.add(safe(r.getWeightedOi()));
        }
    }

    // =========================================================
    // SNAPSHOT (BASE + LIVE)
    // =========================================================
    public Snapshot snapshot(int tf) {

        Bucket b = buckets.get(tf);
        if (b == null) return new Snapshot(0, 0, 0, 0, 0);

        double callFlow = b.baseCallFlow + b.callDpi.sum();
        double putFlow  = b.basePutFlow  + b.putDpi.sum();

        double callOi = b.callWeightedOi.sum();
        double putOi  = b.putWeightedOi.sum();

        return new Snapshot(
                callFlow,
                putFlow,
                callOi,
                putOi,
                callOi - putOi
        );
    }

    // =========================================================
    // OPTIONAL RESET (NOT USED IN CUMULATIVE MODE)
    // =========================================================
    public void reset(int tf) {
        buckets.put(tf, new Bucket());
    }

    // =========================================================
    // INTERNAL BUCKET
    // =========================================================
    static class Bucket {

        // 🔥 BASE (LOADED FROM DB)
        double baseCallFlow = 0;
        double basePutFlow  = 0;

        // 🔥 LIVE INCREMENTAL
        DoubleAdder callDpi = new DoubleAdder();
        DoubleAdder putDpi  = new DoubleAdder();

        DoubleAdder callWeightedOi = new DoubleAdder();
        DoubleAdder putWeightedOi  = new DoubleAdder();
    }

    // =========================================================
    // SNAPSHOT MODEL
    // =========================================================
    public record Snapshot(
            double callDpi,
            double putDpi,
            double callWeightedOi,
            double putWeightedOi,
            double weightedOi
    ) {
        public double netDpi() {
            return callDpi - putDpi;
        }
    }

    // =========================================================
    // NULL / NaN SAFETY
    // =========================================================
    private double safe(Double v) {
        if (v == null || Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        return v;
    }
}

