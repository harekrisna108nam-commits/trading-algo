package com.example.dhan_rsi_series.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.entity.MoneyFlow;
import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.repository.MoneyFlowRepository;
import com.example.dhan_rsi_series.utils.DpiAggregatorService.Bucket;

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

/*@Service
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
}*/

@Service
public class DpiAggregatorService {

	// =========================================================
	// TF -> EXPIRY -> BUCKET
	// =========================================================
	private final Map<Integer, ConcurrentHashMap<LocalDate, Bucket>> buckets = new ConcurrentHashMap<>();

	public static Double netFlow = 0.0;
	
	private final MoneyFlowRepository moneyFlowRepository;

	public DpiAggregatorService(MoneyFlowRepository moneyFlowRepository) {
		this.moneyFlowRepository = moneyFlowRepository;
	}

	// =========================================================
	// 🔥 LOAD LAST FLOW FROM MONEY_FLOW TABLE
	// =========================================================
	@PostConstruct
	public void loadMoneyFlow() {

		try {

			List<MoneyFlow> flows = moneyFlowRepository.findAllActive();

			if (flows == null || flows.isEmpty()) {

				System.out.println("🟡 No MoneyFlow data found → Starting from ZERO");
				return;
			}

			ConcurrentHashMap<LocalDate, Bucket> expiryMap = buckets.computeIfAbsent(5, t -> new ConcurrentHashMap<>());

			for (MoneyFlow flow : flows) {

				LocalDate expiry = flow.getExpiryDate();

				if (expiry == null) {
					continue;
				}

				Bucket bucket = expiryMap.computeIfAbsent(expiry, e -> new Bucket());

				bucket.baseCallFlow = safe(flow.getCallFlow());
				bucket.basePutFlow = safe(flow.getPutFlow());
				bucket.baseFutureFlow = safe(flow.getFutureFlow());

				System.out.println("✅ Restored Flow | Expiry: " + expiry + " | CALL: " + bucket.baseCallFlow
						+ " | PUT: " + bucket.basePutFlow + " | FUTURE: " + bucket.baseFutureFlow);
			}

			System.out.println("✅ MoneyFlow restoration completed");

		} catch (Exception e) {

			System.out.println("❌ Failed loading MoneyFlow table");
			e.printStackTrace();
		}
	}

	// =========================================================
	// 🔥 ADD REAL-TIME DATA
	// =========================================================
	public void add(OptionRsi r, int tf) {

	    if (r.getExpiry() == null) {
	        return;
	    }

	    ConcurrentHashMap<LocalDate, Bucket> expiryMap =
	            buckets.computeIfAbsent(tf, t -> new ConcurrentHashMap<>());

	    Bucket bucket = expiryMap.computeIfAbsent(
	            r.getExpiry(),
	            e -> new Bucket()
	    );

	    // =====================================================
	    // UPDATE LIVE FLOW
	    // =====================================================
	    if ("CALL".equalsIgnoreCase(r.getOptionType())) {

	        bucket.callDpi.add(safe(r.getDpi()));
	        bucket.callWeightedOi.add(safe(r.getWeightedOi()));

	    } else if ("PUT".equalsIgnoreCase(r.getOptionType())) {

	        bucket.putDpi.add(safe(r.getDpi()));
	        bucket.putWeightedOi.add(safe(r.getWeightedOi()));
	    } else if ("FUTURE".equalsIgnoreCase(r.getOptionType())) {
	    	bucket.futureDpi.add(r.getDpi());
	    }

	    // =====================================================
	    // 🔥 AUTO PERSIST AFTER UPDATE
	    // =====================================================
	    persistFlow(tf, r.getExpiry());
	}

	// =========================================================
	// 🔥 SNAPSHOT (TOTAL OF ALL EXPIRIES)
	// =========================================================
	public Snapshot snapshot(int tf) {

		ConcurrentHashMap<LocalDate, Bucket> expiryMap = buckets.get(tf);

		if (expiryMap == null || expiryMap.isEmpty()) {
			return new Snapshot(0, 0, 0, 0, 0, 0, 0, 0);
		}

		double totalCallFlow = 0;
		double totalPutFlow = 0;
		double totalFutureFlow = 0;

		double totalCallOi = 0;
		double totalPutOi = 0;
		
		double totalPrevCallFlow = 0;
		double totalPrevPutFlow = 0;

		// ✅ SUM ALL EXPIRIES
		for (Bucket bucket : expiryMap.values()) {
			
			totalPrevCallFlow += bucket.baseCallFlow;
			
			totalPrevPutFlow += bucket.basePutFlow;

			totalCallFlow += bucket.baseCallFlow + bucket.callDpi.sum();
			totalPutFlow += bucket.basePutFlow + bucket.putDpi.sum();
			totalFutureFlow += bucket.baseFutureFlow + bucket.futureDpi.sum();

			totalCallOi += bucket.callWeightedOi.sum();
			totalPutOi += bucket.putWeightedOi.sum();
		}

		return new Snapshot(totalCallFlow, totalPutFlow, totalPrevCallFlow, totalPrevPutFlow, totalFutureFlow, totalCallOi, totalPutOi, totalCallOi - totalPutOi);
	}
	
	// =========================================================
	// 🔥 SAVE FLOW TO DB BY EXPIRY
	// =========================================================
	private void persistFlow(int tf, LocalDate expiry) {

	    try {

	        ConcurrentHashMap<LocalDate, Bucket> expiryMap = buckets.get(tf);

	        if (expiryMap == null) {
	            return;
	        }

	        Bucket bucket = expiryMap.get(expiry);

	        if (bucket == null) {
	            return;
	        }

	        // =================================================
	        // TOTAL FLOW = BASE + LIVE
	        // =================================================
	        double finalCallFlow =
	                bucket.baseCallFlow + bucket.callDpi.sum();

	        double finalPutFlow =
	                bucket.basePutFlow + bucket.putDpi.sum();
	        
	        double finalFutureFlow = 
	        		bucket.baseFutureFlow + bucket.futureDpi.sum();

	        // =================================================
	        // FIND EXISTING RECORD
	        // =================================================
	        MoneyFlow moneyFlow =
	                moneyFlowRepository.findByExpiryDate(expiry)
	                        .orElseGet(MoneyFlow::new);

	        // =================================================
	        // UPDATE ENTITY
	        // =================================================
	        moneyFlow.setExpiryDate(expiry);
	        moneyFlow.setCallFlow(finalCallFlow);
	        moneyFlow.setPutFlow(finalPutFlow);
	        moneyFlow.setFutureFlow(finalFutureFlow);
	        moneyFlow.setActive(true);
	        moneyFlow.setCreatedAt(LocalDateTime.now());

	        // =================================================
	        // SAVE
	        // =================================================
	        moneyFlowRepository.save(moneyFlow);

	    } catch (Exception e) {

	        System.out.println("❌ Failed to persist MoneyFlow");
	        e.printStackTrace();
	    }
	}

	// =========================================================
	// RESET
	// =========================================================
	public void reset(int tf, LocalDate expiry) {

		ConcurrentHashMap<LocalDate, Bucket> expiryMap = buckets.get(tf);

		if (expiryMap != null) {
			expiryMap.remove(expiry);
		}
	}

	// =========================================================
	// INTERNAL BUCKET
	// =========================================================
	static class Bucket {

		// BASE FROM DB
		double baseCallFlow = 0;
		double basePutFlow = 0;
		double baseFutureFlow = 0;

		// LIVE DPI
		DoubleAdder callDpi = new DoubleAdder();
		DoubleAdder putDpi = new DoubleAdder();
		DoubleAdder futureDpi = new DoubleAdder();

		// OI
		DoubleAdder callWeightedOi = new DoubleAdder();
		DoubleAdder putWeightedOi = new DoubleAdder();
	}

	// =========================================================
	// SNAPSHOT MODEL
	// =========================================================
	public record Snapshot(double currCallDpi, double currPutDpi, double prevCallDpi, double prevPutDpi, double futureDpi, double callWeightedOi, double putWeightedOi,
			double weightedOi) {

		public double netDpi() {
			netFlow = (currCallDpi - currPutDpi) + (futureDpi);
			return netFlow;
		}
	}

	// =========================================================
	// SAFE NULL / NaN
	// =========================================================
	private double safe(Double v) {

		if (v == null || Double.isNaN(v) || Double.isInfinite(v)) {
			return 0.0;
		}

		return v;
	}
}
