package com.example.dhan_rsi_series.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.dhan_rsi_series.entity.DhanSubscription;
import com.example.dhan_rsi_series.entity.OptionChain;
import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.repository.DhanSubscriptionRepository;
import com.example.dhan_rsi_series.repository.OptionChainRepository;
import com.example.dhan_rsi_series.repository.OptionRsiRepository;
import com.example.dhan_rsi_series.utils.CandleSnapshot;
import com.example.dhan_rsi_series.utils.CandleState;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

//@Component
//public class CandleRsiService {
//
//	private final OptionRsiRepository repo;
//
//	private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();
//	private final Map<String, EmaSmoothedRsiCalculator> rsiMap = new ConcurrentHashMap<>();
//	private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();
//	private final Map<String, Double> lastLtpMap = new ConcurrentHashMap<>();
//	private final Map<String, Double> lastRsiMap = new ConcurrentHashMap<>();
//	// keep RSI history per key
//	private final Map<String, double[]> rsiBufferMap = new ConcurrentHashMap<>();
//
//	// ⭐ latest per securityId (IMPORTANT)
//	private final Map<Integer, OptionRsi> latestMap = new ConcurrentHashMap<>();
//	private final Map<String, Double> lastDeltaLtpMap = new ConcurrentHashMap<>();
//
//	public CandleRsiService(OptionRsiRepository repo) {
//		this.repo = repo;
//	}
//
//	public OptionRsi onLtp(String symbol, int securityId, String optionType, double ltp, int oi, int highestOi,
//			double atp, int timeframeSeconds) {
//
//		long now = System.currentTimeMillis() / 1000;
//
//		String key = symbol + "_" + securityId + "_" + timeframeSeconds;
//
//		CandleState candle = candleMap.computeIfAbsent(key, k -> new CandleState(timeframeSeconds));
//
//		candle.onTick(ltp, now);
//
//		if (!candle.isComplete(now))
//			return null;
//
//		CandleSnapshot snap = candle.snapshotAndReset();
//
//		// ================= RSI =================
//
//		EmaSmoothedRsiCalculator calc = rsiMap.computeIfAbsent(key, k -> new EmaSmoothedRsiCalculator(3, 5));
//
//		double rsi = calc.update(snap.close());
//
//		if (rsi == -1)
//			return null;
//
//		// persistent smoothing buffer
//		double[] buf = rsiBufferMap.computeIfAbsent(key, k -> new double[3]);
//
//		buf[0] = buf[1];
//		buf[1] = buf[2];
//		buf[2] = rsi;
//
//		if (buf[0] > 0 && buf[1] > 0 && buf[2] > 0) {
//			rsi = (buf[0] + buf[1] + buf[2]) / 3.0;
//		}
//
//		// ================= SAVE =================
//
//		OptionRsi save = save(symbol, securityId, optionType, timeframeSeconds, snap, rsi, oi, highestOi, atp);
//
//		return save;
//	}
//
//	// =========================================================
//
//	private OptionRsi save(String symbol, int securityId, String optionType, int timeframe, CandleSnapshot c,
//			double rsi, int oi, int highestOi, double atp) {
//
//		double ltp = c.close();
//		String key = symbol + "_" + securityId + "_" + timeframe;
//
//		// ===== previous values =====
//		int prevOi = lastOiMap.getOrDefault(key, oi);
//		double prevLtp = lastLtpMap.getOrDefault(key, ltp);
//		double prevRsi = lastRsiMap.getOrDefault(key, rsi);
//
//		// ===== deltas =====
//		int deltaOi = Math.abs(oi - prevOi);
//		double deltaLtp = ltp - prevLtp;
//		// get previous deltaAtp
//		double prevDeltaLtp = lastDeltaLtpMap.getOrDefault(key, deltaLtp);
//
//		// second derivative (acceleration)
//		double deltaDeltaLtp = deltaLtp - prevDeltaLtp;
//		
//		double dpi = deltaOi * deltaLtp * c.close();
//
//		double deltaRsi = rsi - prevRsi;
//
//		boolean buy = deltaRsi > 0;
//		boolean sell = deltaRsi < 0;
//
//		// ===== update memory =====
//		lastOiMap.put(key, oi);
//		lastLtpMap.put(key, ltp);
//		lastRsiMap.put(key, rsi);
//		lastDeltaLtpMap.put(key, deltaLtp);
//		// ===== persist =====
//		OptionRsi e = new OptionRsi();
//
//		e.setSymbol(symbol);
//		e.setSecurityId(securityId);
//		e.setOptionType(optionType);
//
//		e.setTimeframe(toFrame(timeframe));
//
//		e.setCandleTime(Instant.ofEpochSecond(c.startEpoch()).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
//
//		e.setOpen(c.open());
//		e.setHigh(c.high());
//		e.setLow(c.low());
//		e.setClose(c.close());
//
//		e.setRsi(rsi);
//
//		e.setOi(oi);
//		e.setHighestOi(highestOi);
//		e.setAtp(atp);
//
//		e.setDpi(dpi);
//		e.setDeltaRsi(deltaRsi);
//
//		e.setBuy(buy);
//		e.setSell(sell);
//		e.setDeltaLtp(deltaLtp);
//		e.setDeltaDeltaLtp(deltaDeltaLtp);
//		// ⭐ store latest by securityId
//		latestMap.put(securityId, e);
//		// repo.save(e);
//
//		System.out.printf("%s %s RSI=%.2f ΔRSI=%.2f DPI=%.2f OI=%d H-OI=%d ATP=%.2f%n", optionType, e.getTimeframe(),
//				rsi, deltaRsi, dpi, oi, highestOi, atp);
//
//		return e;
//	}
//
//	// =========================================================
//
//	private String toFrame(int tf) {
//		return switch (tf) {
//		case 5 -> "5S";
//		case 30 -> "30S";
//		case 60 -> "1M";
//		default -> tf + "S";
//		};
//	}
//
//	public OptionRsi latest(int securityId) {
//		return latestMap.get(securityId);
//	}
//}

//@Component
//public class CandleRsiService {
//
//    private final OptionRsiRepository repo;
//
//    private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();
//
//    private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastLtpMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastDeltaLtpMap = new ConcurrentHashMap<>();
//
//    // warmup counter (IMPORTANT)
//    private final Map<String, Integer> warmupCountMap = new ConcurrentHashMap<>();
//
//    // latest per securityId
//    private final Map<Integer, OptionRsi> latestMap = new ConcurrentHashMap<>();
//
//    public CandleRsiService(OptionRsiRepository repo) {
//        this.repo = repo;
//    }
//
//    // =========================================================
//
//    public OptionRsi onLtp(String symbol, int securityId, String optionType,
//                           double ltp, int oi, int highestOi,
//                           double atp, int timeframeSeconds) {
//
//        long now = System.currentTimeMillis() / 1000;
//        String key = symbol + "_" + securityId + "_" + timeframeSeconds;
//
//        CandleState candle = candleMap.computeIfAbsent(
//                key, k -> new CandleState(timeframeSeconds)
//        );
//
//        candle.onTick(ltp, now);
//
//        if (!candle.isComplete(now)) return null;
//
//        CandleSnapshot snap = candle.snapshotAndReset();
//
//        // ===== Warmup Logic (3 candles) =====
//        int count = warmupCountMap.getOrDefault(key, 0) + 1;
//        warmupCountMap.put(key, count);
//
//        if (count < 3) {
//            updateMemory(key, snap.close(), oi);
//            return null;
//        }
//
//        // ===== After warmup =====
//        return save(symbol, securityId, optionType, timeframeSeconds,
//                snap, oi, highestOi, atp);
//    }
//
//    // =========================================================
//
//    private void updateMemory(String key, double ltp, int oi) {
//        lastLtpMap.put(key, ltp);
//        lastOiMap.put(key, oi);
//        lastDeltaLtpMap.put(key, 0.0);
//    }
//
//    // =========================================================
//
//    private OptionRsi save(String symbol, int securityId, String optionType,
//                           int timeframe, CandleSnapshot c,
//                           int oi, int highestOi, double atp) {
//
//        double ltp = c.close();
//        String key = symbol + "_" + securityId + "_" + timeframe;
//
//        // ===== previous values =====
//        int prevOi = lastOiMap.getOrDefault(key, oi);
//        double prevLtp = lastLtpMap.getOrDefault(key, ltp);
//
//        // ===== deltas =====
//        int deltaOi = Math.abs(oi - prevOi);
//        double deltaLtp = ltp - prevLtp;
//
//        double prevDeltaLtp = lastDeltaLtpMap.getOrDefault(key, deltaLtp);
//
//        // second derivative (acceleration)
//        double deltaDeltaLtp = deltaLtp - prevDeltaLtp;
//
//        double dpi = deltaOi * deltaLtp * ltp;
//
//        // ===== update memory =====
//        lastOiMap.put(key, oi);
//        lastLtpMap.put(key, ltp);
//        lastDeltaLtpMap.put(key, deltaLtp);
//
//        // ===== persist =====
//        OptionRsi e = new OptionRsi();
//
//        e.setSymbol(symbol);
//        e.setSecurityId(securityId);
//        e.setOptionType(optionType);
//        e.setTimeframe(toFrame(timeframe));
//
//        e.setCandleTime(
//                Instant.ofEpochSecond(c.startEpoch())
//                        .atZone(ZoneId.of("Asia/Kolkata"))
//                        .toLocalDateTime()
//        );
//
//        e.setOpen(c.open());
//        e.setHigh(c.high());
//        e.setLow(c.low());
//        e.setClose(c.close());
//
//        e.setOi(oi);
//        e.setHighestOi(highestOi);
//        e.setAtp(atp);
//
//        e.setDpi(dpi);
//        e.setDeltaLtp(deltaLtp);
//        e.setDeltaDeltaLtp(deltaDeltaLtp);
//
//        // simple signals
//        e.setBuy(deltaDeltaLtp > 0);
//        e.setSell(deltaDeltaLtp < 0);
//
//        // store latest
//        latestMap.put(securityId, e);
//
//        // optional DB save
//        // repo.save(e);
//
//        System.out.printf(
//                "%s %s LTP=%.2f ΔLTP=%.2f ΔΔLTP=%.2f DPI=%.2f OI=%d%n",
//                optionType,
//                e.getTimeframe(),
//                ltp,
//                deltaLtp,
//                deltaDeltaLtp,
//                dpi,
//                oi
//        );
//
//        return e;
//    }
//
//    // =========================================================
//
//    private String toFrame(int tf) {
//        return switch (tf) {
//            case 5 -> "5S";
//            case 30 -> "30S";
//            case 60 -> "1M";
//            default -> tf + "S";
//        };
//    }
//
//    // =========================================================
//
//    public OptionRsi latest(int securityId) {
//        return latestMap.get(securityId);
//    }
//}


//@Component
//public class CandleRsiService {
//
//    private final OptionRsiRepository repo;
//
//    private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();
//
//    private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastLtpMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastDeltaLtpMap = new ConcurrentHashMap<>();
//
//    private final Map<String, Integer> warmupCountMap = new ConcurrentHashMap<>();
//
//    private final Map<Integer, OptionRsi> latestMap = new ConcurrentHashMap<>();
//
//    public CandleRsiService(OptionRsiRepository repo) {
//        this.repo = repo;
//    }
//
//    // =========================================================
//
//    public OptionRsi onLtp(String symbol, int securityId, String optionType,
//                           double ltp, int oi, int highestOi,
//                           double atp, int timeframeSeconds) {
//
//        long now = System.currentTimeMillis() / 1000;
//        String key = buildKey(symbol, securityId, timeframeSeconds);
//
//        CandleState candle = candleMap.computeIfAbsent(
//                key, k -> new CandleState(timeframeSeconds)
//        );
//
//        candle.onTick(ltp, now);
//
//        if (!candle.isComplete(now)) return null;
//
//        // ✅ FIXED: pass now + ltp
//        CandleSnapshot snap = candle.snapshotAndReset(now, ltp);
//
//        // ===== Warmup =====
//        int count = warmupCountMap.getOrDefault(key, 0) + 1;
//        warmupCountMap.put(key, count);
//
//        if (count <= 2) {
//            updateMemory(key, snap.close(), oi);
//            return null;
//        }
//
//        return save(symbol, securityId, optionType, timeframeSeconds,
//                snap, oi, highestOi, atp);
//    }
//
//    // =========================================================
//
//    private String buildKey(String symbol, int securityId, int tf) {
//        return symbol + "_" + securityId + "_" + tf;
//    }
//
//    // =========================================================
//
//    private void updateMemory(String key, double ltp, int oi) {
//        lastLtpMap.put(key, ltp);
//        lastOiMap.put(key, oi);
//        lastDeltaLtpMap.put(key, 0.0);
//    }
//
//    // =========================================================
//
//    private OptionRsi save(String symbol, int securityId, String optionType,
//                           int timeframe, CandleSnapshot c,
//                           int oi, int highestOi, double atp) {
//
//        double ltp = c.close();
//        String key = buildKey(symbol, securityId, timeframe);
//
//        // ✅ FIRST TIME PROTECTION
//        if (!lastLtpMap.containsKey(key)) {
//            updateMemory(key, ltp, oi);
//            return null;
//        }
//
//        int prevOi = lastOiMap.getOrDefault(key, oi);
//        double prevLtp = lastLtpMap.getOrDefault(key, ltp);
//        double prevDeltaLtp = lastDeltaLtpMap.getOrDefault(key, 0.0);
//
//        double deltaLtp = ltp - prevLtp;
//        int deltaOi = oi - prevOi;
//
//        double deltaDeltaLtp = deltaLtp - prevDeltaLtp;
//
//        double dpi = deltaOi * deltaLtp * ltp;
//
//        // ✅ DEBUG (keep this ON for now)
//        System.out.printf(
//                "DEBUG key=%s prev=%.2f curr=%.2f Δ=%.4f%n",
//                key, prevLtp, ltp, deltaLtp
//        );
//
//        // update memory AFTER calculation
//        lastOiMap.put(key, oi);
//        lastLtpMap.put(key, ltp);
//        lastDeltaLtpMap.put(key, deltaLtp);
//
//        OptionRsi e = new OptionRsi();
//
//        e.setSymbol(symbol);
//        e.setSecurityId(securityId);
//        e.setOptionType(optionType);
//        e.setTimeframe(toFrame(timeframe));
//
//        e.setCandleTime(
//                Instant.ofEpochSecond(c.startEpoch())
//                        .atZone(ZoneId.of("Asia/Kolkata"))
//                        .toLocalDateTime()
//        );
//
//        e.setOpen(c.open());
//        e.setHigh(c.high());
//        e.setLow(c.low());
//        e.setClose(c.close());
//
//        e.setOi(oi);
//        e.setHighestOi(highestOi);
//        e.setAtp(atp);
//
//        e.setDpi(dpi);
//        e.setDeltaLtp(deltaLtp);
//        e.setDeltaDeltaLtp(deltaDeltaLtp);
//
//        // ✅ improved signals
//        e.setBuy(deltaLtp > 0 && deltaDeltaLtp > 0);
//        e.setSell(deltaLtp < 0 && deltaDeltaLtp < 0);
//
//        latestMap.put(securityId, e);
//
//        // repo.save(e);
//
//        System.out.printf(
//                "%s %s LTP=%.2f Δ=%.4f ΔΔ=%.4f DPI=%.2f%n",
//                optionType,
//                e.getTimeframe(),
//                ltp,
//                deltaLtp,
//                deltaDeltaLtp,
//                dpi
//        );
//
//        return e;
//    }
//
//    // =========================================================
//
//    private String toFrame(int tf) {
//        return switch (tf) {
//            case 5 -> "5S";
//            case 30 -> "30S";
//            case 60 -> "1M";
//            default -> tf + "S";
//        };
//    }
//
//    // =========================================================
//
//    public OptionRsi latest(int securityId) {
//        return latestMap.get(securityId);
//    }
//}

//@Component
//public class CandleRsiService {
//
//    private final OptionRsiRepository repo;
//
//    private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();
//
//    private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastLtpMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastDeltaLtpMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastNiftySpotPrice = new ConcurrentHashMap<>();
//    
//
//    private final Map<Integer, OptionRsi> latestMap = new ConcurrentHashMap<>();
//
//    public CandleRsiService(OptionRsiRepository repo) {
//        this.repo = repo;
//    }
//
//    // =========================================================
//
//    public OptionRsi onLtp(String symbol, int securityId, String optionType,
//                           double ltp, int oi, int highestOi,
//                           double atp, int timeframeSeconds) {
//
//        long now = System.currentTimeMillis() / 1000;
//        String key = buildKey(symbol, securityId, timeframeSeconds);
//
//        CandleState candle = candleMap.computeIfAbsent(
//                key, k -> new CandleState(timeframeSeconds)
//        );
//
//        candle.onTick(ltp, now);
//
//        if (!candle.isComplete(now)) return null;
//
//        CandleSnapshot snap = candle.snapshotAndReset(now, ltp);
//
//        return process(symbol, securityId, optionType, timeframeSeconds,
//                snap, oi, highestOi, atp, candle.getClose());
//    }
//
//    // =========================================================
//
//    private String buildKey(String symbol, int securityId, int tf) {
//        return symbol + "_" + securityId + "_" + tf;
//    }
//
//    // =========================================================
//
//    private OptionRsi process(String symbol, int securityId, String optionType,
//                              int timeframe, CandleSnapshot c,
//                              int oi, int highestOi, double atp, double ltp) {
//    	
//    	
//
//        //double ltp = c.close();
//        String key = buildKey(symbol, securityId, timeframe);
//
//        Double prevLtp = lastLtpMap.get(key);
//        Double prevDeltaLtp = lastDeltaLtpMap.get(key);
//        Double prevNiftySpotPrice = lastNiftySpotPrice.get(key);
//        
//        if (prevNiftySpotPrice==null && "SPOT".equalsIgnoreCase(optionType)) {
//            lastNiftySpotPrice.put(key, ltp);
//            return null;
//        }
//        
//        double deltaSpot = Math.abs(ltp - prevNiftySpotPrice);
//
//        // =========================
//        // 🥇 STAGE 1 → ONLY LTP
//        // =========================
//        if (prevLtp == null) {
//            lastLtpMap.put(key, ltp);
//            lastOiMap.put(key, oi);
//
//            System.out.printf("Stage1 [%s] LTP=%.2f%n", key, ltp);
//            return null;
//        }
//
//        // =========================
//        // 🥈 STAGE 2 → DELTA LTP
//        // =========================
//        double deltaLtp = ltp - prevLtp;
//
//        if (prevDeltaLtp == null) {
//            lastLtpMap.put(key, ltp);
//            lastOiMap.put(key, oi);
//            lastDeltaLtpMap.put(key, deltaLtp);
//
//            System.out.printf("Stage2 [%s] ΔLTP=%.4f%n", key, deltaLtp);
//            return null;
//        }
//
//        // =========================
//        // 🥉 STAGE 3 → ACCELERATION
//        // =========================
//        double deltaDeltaLtp = deltaLtp - prevDeltaLtp;
//
//        int prevOi = lastOiMap.getOrDefault(key, oi);
//        int deltaOi = oi - prevOi;
//
//        double dpi = deltaOi * deltaLtp * ltp;
//
//        // ===== update memory =====
//        lastLtpMap.put(key, ltp);
//        lastOiMap.put(key, oi);
//        lastDeltaLtpMap.put(key, deltaLtp);
//
//        OptionRsi e = new OptionRsi();
//
//        e.setSymbol(symbol);
//        e.setSecurityId(securityId);
//        e.setOptionType(optionType);
//        e.setTimeframe(toFrame(timeframe));
//
//        e.setCandleTime(
//                Instant.ofEpochSecond(c.startEpoch())
//                        .atZone(ZoneId.of("Asia/Kolkata"))
//                        .toLocalDateTime()
//        );
//
//        e.setOpen(c.open());
//        e.setHigh(c.high());
//        e.setLow(c.low());
//        e.setClose(c.close());
//
//        e.setOi(oi);
//        e.setHighestOi(highestOi);
//        e.setAtp(atp);
//
//        e.setDpi(dpi);
//        e.setDeltaLtp(deltaLtp);
//        e.setDeltaDeltaLtp(deltaDeltaLtp);
//
//        e.setBuy(deltaLtp > 0 && deltaDeltaLtp > 0);
//        e.setSell(deltaLtp < 0 && deltaDeltaLtp < 0);
//
//        latestMap.put(securityId, e);
//
//        System.out.printf(
//                "Stage3 [%s] LTP=%.2f Δ=%.4f ΔΔ=%.4f DPI=%.2f%n",
//                key, ltp, deltaLtp, deltaDeltaLtp, dpi
//        );
//
//        return e;
//    }
//
//    // =========================================================
//
//    private String toFrame(int tf) {
//        return switch (tf) {
//            case 5 -> "5S";
//            case 30 -> "30S";
//            case 60 -> "1M";
//            default -> tf + "S";
//        };
//    }
//
//    // =========================================================
//
//    public OptionRsi latest(int securityId) {
//        return latestMap.get(securityId);
//    }
//}

/*@Slf4j
@Component
public class CandleRsiService {

    private final OptionRsiRepository repo;

    private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();
    private final Map<String, Double> lastLtpMap = new ConcurrentHashMap<>();
    private final Map<String, Double> lastDeltaLtpMap = new ConcurrentHashMap<>();
    private final Map<Integer, OptionRsi> latestMap = new ConcurrentHashMap<>();

    // ================= FUTURE STATE =================
    private volatile double lastFuturePrice = 0.0;
    private volatile double prevFuturePrice = 0.0;
    private volatile double deltaFuture = 0.0; // dynamic

    public CandleRsiService(OptionRsiRepository repo) {
        this.repo = repo;
    }

    // =========================================================
    public OptionRsi onLtp(String symbol,
                           int securityId,
                           String optionType,
                           double ltp,
                           int oi,
                           int highestOi,
                           double atp,
                           int timeframeSeconds) {

        long now = System.currentTimeMillis() / 1000;
        String key = buildKey(symbol, securityId, timeframeSeconds);

        CandleState candle = candleMap.computeIfAbsent(
                key, k -> new CandleState(timeframeSeconds)
        );

        candle.onTick(ltp, now);

        if (!candle.isComplete(now)) return null;

        CandleSnapshot snap = candle.snapshotAndReset(now, ltp);

        // =====================================================
        // ✅ FUTURE (1 MIN)
        // =====================================================
        if ("FUTURE".equalsIgnoreCase(optionType)) {

            if (lastFuturePrice == 0.0) {
                lastFuturePrice = snap.close();
                prevFuturePrice = lastFuturePrice;
                log.info("FUTURE INIT {}", lastFuturePrice);
                return null;
            }

            prevFuturePrice = lastFuturePrice;
            lastFuturePrice = snap.close();

            double newDelta = Math.abs(lastFuturePrice - prevFuturePrice);

            // EMA smoothing
            deltaFuture = (deltaFuture == 0.0)
                    ? newDelta
                    : (0.7 * deltaFuture + 0.3 * newDelta);

            log.info("FUTURE Δ RAW={} SMOOTHED={}", newDelta, deltaFuture);

            return null;
        }

        // =====================================================
        // ✅ OPTIONS (5 SEC)
        // =====================================================
        return process(symbol, securityId, optionType, timeframeSeconds,
                snap, oi, highestOi, atp);
    }

    // =========================================================
    private String buildKey(String symbol, int securityId, int tf) {
        return symbol + "_" + securityId + "_" + tf;
    }

    // =========================================================
    private OptionRsi process(String symbol,
                              int securityId,
                              String optionType,
                              int timeframe,
                              CandleSnapshot c,
                              int oi,
                              int highestOi,
                              double atp) {

        String key = buildKey(symbol, securityId, timeframe);

        double ltp = c.close();

        Double prevLtp = lastLtpMap.get(key);
        Double prevDeltaLtp = lastDeltaLtpMap.get(key);

        // =========================
        // STAGE 1
        // =========================
        if (prevLtp == null) {
            lastLtpMap.put(key, ltp);
            lastOiMap.put(key, oi);
            return null;
        }

        double deltaLtp = ltp - prevLtp;

        // =========================
        // STAGE 2
        // =========================
        if (prevDeltaLtp == null) {
            lastLtpMap.put(key, ltp);
            lastOiMap.put(key, oi);
            lastDeltaLtpMap.put(key, deltaLtp);
            return null;
        }

        double deltaDeltaLtp = deltaLtp - prevDeltaLtp;

        int prevOi = lastOiMap.getOrDefault(key, oi);
        int deltaOi = oi - prevOi;

        // =====================================================
        // 🔥 DYNAMIC SAFE DELTA FUTURE (FIXED)
        // =====================================================
        double safeDeltaFuture = (deltaFuture > 0.5)
                ? deltaFuture
                : Math.max(ltp * 0.001, 1.0);

        double Ys = Math.abs(deltaLtp) / safeDeltaFuture;

        double flow = Math.abs(deltaOi) * Ys * ltp;

        // =========================
        // UPDATE MEMORY
        // =========================
        lastLtpMap.put(key, ltp);
        lastOiMap.put(key, oi);
        lastDeltaLtpMap.put(key, deltaLtp);

        OptionRsi e = new OptionRsi();

        e.setSymbol(symbol);
        e.setSecurityId(securityId);
        e.setOptionType(optionType);
        e.setTimeframe(toFrame(timeframe));

        e.setCandleTime(
                Instant.ofEpochSecond(c.startEpoch())
                        .atZone(ZoneId.of("Asia/Kolkata"))
                        .toLocalDateTime()
        );

        e.setOpen(c.open());
        e.setHigh(c.high());
        e.setLow(c.low());
        e.setClose(c.close());

        e.setOi(oi);
        e.setHighestOi(highestOi);
        e.setAtp(atp);

        e.setDpi(flow);
        e.setDeltaLtp(deltaLtp);
        e.setDeltaDeltaLtp(deltaDeltaLtp);

        latestMap.put(securityId, e);

        return e;
    }

    private String toFrame(int tf) {
        return switch (tf) {
            case 5 -> "5S";
            case 60 -> "1M";
            default -> tf + "S";
        };
    }

    public OptionRsi latest(int securityId) {
        return latestMap.get(securityId);
    }
}*/

/*@Slf4j
@Component
public class CandleRsiService {

    private final OptionRsiRepository repo;

    // ================= STATE =================
    private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();
    private final Map<String, Double> lastLtpMap = new ConcurrentHashMap<>();
    private final Map<String, Double> lastDeltaLtpMap = new ConcurrentHashMap<>();

    // FUTURE
    private volatile double prevFutureClose = 0.0;
    private volatile double currFutureClose = 0.0;
    private volatile double deltaFuture = 10.0; // SAFE DEFAULT

    // GAMMA
    private final Map<Integer, Double> gammaMap = new ConcurrentHashMap<>();
    private final Map<Integer, Double> prevStrikeClose = new ConcurrentHashMap<>();

    // 5 tick buffer
    private final Map<Integer, Deque<Double>> ltpBuffer = new ConcurrentHashMap<>();

    private final Map<Integer, OptionRsi> latestMap = new ConcurrentHashMap<>();

    public CandleRsiService(OptionRsiRepository repo) {
        this.repo = repo;
    }

    // =========================================================
    public OptionRsi onLtp(String symbol, int securityId, String optionType,
                          double ltp, int oi, int highestOi,
                          double atp, int timeframeSeconds) {

        long now = System.currentTimeMillis() / 1000;
        String key = symbol + "_" + securityId + "_" + timeframeSeconds;

        CandleState candle = candleMap.computeIfAbsent(
                key, k -> new CandleState(timeframeSeconds)
        );

        candle.onTick(ltp, now);

        if (!candle.isComplete(now)) return null;

        CandleSnapshot snap = candle.snapshotAndReset(now, ltp);

        // =====================================================
        // ✅ FUTURE (1 MIN)
        // =====================================================
        if ("FUTURE".equalsIgnoreCase(optionType)) {

            prevFutureClose = currFutureClose;
            currFutureClose = snap.close();

            if (prevFutureClose != 0.0) {
                double raw = Math.abs(currFutureClose - prevFutureClose);
                deltaFuture = 0.7 * deltaFuture + 0.3 * raw; // EMA
            }

            log.info("FUTURE Δ={}", deltaFuture);
            return null;
        }

        // =====================================================
        // ✅ OPTIONS (5s)
        // =====================================================
        return process(symbol, securityId, optionType,
                timeframeSeconds, snap, oi, highestOi, atp);
    }

    // =========================================================
    private OptionRsi process(String symbol, int securityId, String optionType,
                             int timeframe, CandleSnapshot c,
                             int oi, int highestOi, double atp) {

        String key = symbol + "_" + securityId + "_" + timeframe;

        double ltp = c.close();

        // =========================
        // 🔥 GAMMA
        // =========================
        double prevClose = prevStrikeClose.getOrDefault(securityId, ltp);
        double deltaStrike = Math.abs(ltp - prevClose);
        prevStrikeClose.put(securityId, ltp);

        double safeDeltaFuture = (deltaFuture <= 0.0) ? 10.0 : deltaFuture;
        double gamma = deltaStrike / safeDeltaFuture;

        gammaMap.put(securityId, gamma);

        // =========================
        // 🔥 5-TICK AVG
        // =========================
        Deque<Double> buffer = ltpBuffer.computeIfAbsent(securityId, k -> new ArrayDeque<>());
        buffer.addLast(ltp);
        if (buffer.size() > 5) buffer.pollFirst();

        if (buffer.size() < 5) return null;

        double avgLtp = buffer.stream().mapToDouble(Double::doubleValue).average().orElse(ltp);
        double lastLtp = buffer.peekLast();

        // =========================
        // STAGE 1
        // =========================
        Double prevLtp = lastLtpMap.get(key);
        if (prevLtp == null) {
            lastLtpMap.put(key, ltp);
            lastOiMap.put(key, oi);
            return null;
        }

        double deltaLtp = ltp - prevLtp;

        // =========================
        // STAGE 2
        // =========================
        Double prevDeltaLtp = lastDeltaLtpMap.get(key);
        if (prevDeltaLtp == null) {
            lastLtpMap.put(key, ltp);
            lastOiMap.put(key, oi);
            lastDeltaLtpMap.put(key, deltaLtp);
            return null;
        }

        double deltaDeltaLtp = deltaLtp - prevDeltaLtp;

        int prevOi = lastOiMap.getOrDefault(key, oi);
        int deltaOi = oi - prevOi;

        // =========================
        // 🔥 FLOW LOGIC
        // =========================
        double sign = (lastLtp > avgLtp) ? 1 : -1;

        double dpi = sign * Math.abs(deltaOi) * gamma * avgLtp;
        double weightedOi = sign * Math.abs(deltaOi) * gamma;

        // =========================
        // UPDATE MEMORY
        // =========================
        lastLtpMap.put(key, ltp);
        lastOiMap.put(key, oi);
        lastDeltaLtpMap.put(key, deltaLtp);

        OptionRsi e = new OptionRsi();

        e.setSymbol(symbol);
        e.setSecurityId(securityId);
        e.setOptionType(optionType);
        e.setTimeframe(timeframe == 5 ? "5S" : "1M");

        e.setCandleTime(
                Instant.ofEpochSecond(c.startEpoch())
                        .atZone(ZoneId.of("Asia/Kolkata"))
                        .toLocalDateTime()
        );

        e.setOpen(c.open());
        e.setHigh(c.high());
        e.setLow(c.low());
        e.setClose(c.close());

        e.setOi(oi);
        e.setHighestOi(highestOi);
        e.setAtp(atp);

        e.setDpi(dpi);
        e.setWeightedOi(weightedOi);
        e.setDeltaLtp(deltaLtp);
        e.setDeltaDeltaLtp(deltaDeltaLtp);

        latestMap.put(securityId, e);

        return e;
    }

    public OptionRsi latest(int securityId) {
        return latestMap.get(securityId);
    }
}*/

//@Slf4j
//@Component
//public class CandleRsiService {
//
//    private final OptionRsiRepository repo;
//
//    // ================= COMMON STATE =================
//    private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();
//    private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastLtpMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastDeltaLtpMap = new ConcurrentHashMap<>();
//
//    // ================= FUTURE (1 MIN) =================
//    private volatile double prevFutureClose = 0.0;
//    private volatile double currFutureClose = 0.0;
//    private volatile double deltaFuture = 10.0; // safe default
//
//    // ================= GAMMA (1 MIN ONLY) =================
//    private final Map<Integer, Double> prevStrikeClose1m = new ConcurrentHashMap<>();
//    private final Map<Integer, Double> currStrikeClose1m = new ConcurrentHashMap<>();
//    private final Map<Integer, Double> gammaMap = new ConcurrentHashMap<>();
//
//    // ================= 5-TICK BUFFER =================
//    private final Map<Integer, Deque<Double>> ltpBuffer = new ConcurrentHashMap<>();
//
//    private final Map<Integer, OptionRsi> latestMap = new ConcurrentHashMap<>();
//
//    public CandleRsiService(OptionRsiRepository repo) {
//        this.repo = repo;
//    }
//
//    // =========================================================
//    public OptionRsi onLtp(String symbol, int securityId, String optionType,
//                          double ltp, int oi, int highestOi,
//                          double atp, int timeframeSeconds) {
//
//        long now = System.currentTimeMillis() / 1000;
//        String key = symbol + "_" + securityId + "_" + timeframeSeconds;
//        
//        CandleState candle = candleMap.computeIfAbsent(
//                key, k -> new CandleState(timeframeSeconds)
//        );
//
//        candle.onTick(ltp, now);
//
//        if (!candle.isComplete(now)) return null;
//
//        CandleSnapshot snap = candle.snapshotAndReset(now, ltp);
//
//        // =====================================================
//        // ✅ FUTURE (1 MIN)
//        // =====================================================
//        if ("FUTURE".equalsIgnoreCase(optionType)) {
//
//            prevFutureClose = currFutureClose;
//            currFutureClose = snap.close();
//
//            if (prevFutureClose != 0.0) {
//                double raw = Math.abs(currFutureClose - prevFutureClose);
//                deltaFuture = 0.7 * deltaFuture + 0.3 * raw;
//            }
//
//            log.info("FUTURE Δ={}", deltaFuture);
//            return null;
//        }
//
//        // =====================================================
//        // ✅ GAMMA UPDATE (ONLY 1 MIN)
//        // =====================================================
//        if (timeframeSeconds == 60) {
//
//            prevStrikeClose1m.put(
//                    securityId,
//                    currStrikeClose1m.getOrDefault(securityId, snap.close())
//            );
//
//            currStrikeClose1m.put(securityId, snap.close());
//
//            double prev = prevStrikeClose1m.get(securityId);
//            double curr = currStrikeClose1m.get(securityId);
//
//            double deltaStrike1m = Math.abs(curr - prev);
//
//            double safeDeltaFuture = Math.max(deltaFuture, 5.0);
//
//            double gamma = deltaStrike1m / safeDeltaFuture;
//
//            gammaMap.put(securityId, gamma);
//
//            log.info("GAMMA [{}] = {}", securityId, gamma);
//
//            return null;
//        }
//
//        // =====================================================
//        //  OPTIONS FLOW (5 SEC)
//        // =====================================================
//        return process(symbol, securityId, optionType,
//                timeframeSeconds, snap, oi, highestOi, atp);
//    }
//
//    // =========================================================
//    private OptionRsi process(String symbol,
//                             int securityId,
//                             String optionType,
//                             int timeframe,
//                             CandleSnapshot c,
//                             int oi,
//                             int highestOi,
//                             double atp) {
//
//        String key = symbol + "_" + securityId + "_" + timeframe;
//
//        double ltp = c.close();
//
//        // =========================
//        // 🔥 USE GAMMA (FROM 1 MIN)
//        // =========================
//        double gamma = gammaMap.getOrDefault(securityId, 0.0);
//
//        if (gamma < 0.01) return null; // wait for valid gamma
//
//        
//
//        // =========================
//        Integer prevOi = lastOiMap.getOrDefault(key, oi);
//        if (prevOi == null) {
//            //lastLtpMap.put(key, ltp);
//            lastOiMap.put(key, oi);
//            return null;
//        }
//
//        //double deltaLtp = ltp - prevLtp;
//
////        Double prevDeltaLtp = lastDeltaLtpMap.get(key);
////        if (prevDeltaLtp == null) {
////            lastLtpMap.put(key, ltp);
////            lastOiMap.put(key, oi);
////            lastDeltaLtpMap.put(key, deltaLtp);
////            return null;
////        }
////
////        double deltaDeltaLtp = deltaLtp - prevDeltaLtp;
//
//        
//        int deltaOi = oi - prevOi;
//
//        // =========================
//        // 🔥 FINAL FLOW
//        // =========================
//        double sign = (ltp > c.avgLtp()) ? 1 : -1;
//
//        double dpi = sign * Math.abs(deltaOi) * gamma * c.avgLtp();
//        double weightedOi = sign * Math.abs(deltaOi) * gamma;
//
//        // =========================
//        //lastLtpMap.put(key, ltp);
//        lastOiMap.put(key, oi);
//        //lastDeltaLtpMap.put(key, deltaLtp);
//
//        OptionRsi e = new OptionRsi();
//
//        e.setSymbol(symbol);
//        e.setSecurityId(securityId);
//        e.setOptionType(optionType);
//        e.setTimeframe("5S");
//
//        e.setCandleTime(
//                Instant.ofEpochSecond(c.startEpoch())
//                        .atZone(ZoneId.of("Asia/Kolkata"))
//                        .toLocalDateTime()
//        );
//
//        e.setOpen(c.open());
//        e.setHigh(c.high());
//        e.setLow(c.low());
//        e.setClose(c.close());
//
//        e.setOi(oi);
//        e.setHighestOi(highestOi);
//        e.setAtp(atp);
//
//        e.setDpi(dpi);
//        e.setWeightedOi(weightedOi);
//        e.setGamma(gamma);
//        
//        //e.setDeltaLtp(deltaLtp);
//        //e.setDeltaDeltaLtp(deltaDeltaLtp);
//
//        latestMap.put(securityId, e);
//
//        return e;
//    }
//
//    public OptionRsi latest(int securityId) {
//        return latestMap.get(securityId);
//    }
//}

//@Slf4j
//@Component
//public class CandleRsiService {
//
//    private final OptionRsiRepository repo;
//    private final DhanSubscriptionRepository subscriptionRepository;
//
//    private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();
//    private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();
//    private final Map<String, Double> lastClosePrice = new ConcurrentHashMap<>();
//    
//
//    // ================= FUTURE =================
//    private volatile double prevFutureClose = 0.0;
//    private volatile double currFutureClose = 0.0;
//    private volatile double deltaFuture = 0.0;
//
//    // ================= GAMMA =================
//    private final Map<Integer, Double> prevStrikeClose1m = new ConcurrentHashMap<>();
//    private final Map<Integer, Double> currStrikeClose1m = new ConcurrentHashMap<>();
//
//    // 🔥 FINAL GAMMA USED (ONLY 11th, 21st...)
//    private final Map<Integer, Double> selectedGammaMap = new ConcurrentHashMap<>();
//
//    // 🔥 COUNTER PER SECURITY
//    private final Map<Integer, Integer> minuteCounterMap = new ConcurrentHashMap<>();
//
//    public final Map<Integer, OptionRsi> latestMap = new ConcurrentHashMap<>();
//    
//    public final Map<Integer, OptionRsi> fourthSecOptionMap = new ConcurrentHashMap<>();
//    private final Set<Integer> pendingGammaSet = ConcurrentHashMap.newKeySet();
//    
//    public CandleRsiService(OptionRsiRepository repo, DhanSubscriptionRepository subscriptionRepository) {
//        this.repo = repo;
//        this.subscriptionRepository = subscriptionRepository;
//    }
//
//    // =========================================================
//    public OptionRsi onLtp(String symbol, int securityId, String optionType,
//                          double ltp, int oi, int highestOi,
//                          double atp, int timeframeSeconds) {
//
//        long now = System.currentTimeMillis() / 1000;
//        String key = symbol + "_" + securityId + "_" + timeframeSeconds;
//
//        CandleState candle = candleMap.computeIfAbsent(
//                key, k -> new CandleState(timeframeSeconds)
//        );
//
//        candle.onTick(ltp, now);
//
//        if (!candle.isComplete(now)) return null;
//
//        CandleSnapshot snap = candle.snapshotAndReset(now, ltp);
//
//     // =========================================
//        // 🔥 4-SEC FLOW → STORE + TRY GAMMA
//        // =========================================
//        if (timeframeSeconds == 4) {
//
//            OptionRsi rsi4 = new OptionRsi();
//            rsi4.setSecurityId(securityId);
//            rsi4.setOptionType(optionType);
//            rsi4.setClose(snap.close());
//
//            rsi4.setCandleTime(
//                Instant.ofEpochSecond(snap.startEpoch())
//                    .atZone(ZoneId.of("Asia/Kolkata"))
//                    .toLocalDateTime()
//            );
//
//            fourthSecOptionMap.put(securityId, rsi4);
//
//            boolean success = tryCalculateGamma(securityId, optionType);
//
//            if (!success) {
//                pendingGammaSet.add(securityId);
//            } else {
//                pendingGammaSet.remove(securityId);
//            }
//
//            // ✅ 3. 🔥 RECOMPUTE HERE
//            recomputePendingGamma();
//
//            return null;
//        }
//        
//        // =====================================================
//        // 5 SECOND FLOW
//        // =====================================================
//        return process(symbol, securityId, optionType,
//                snap, oi, highestOi, atp);
//    }
//    
//    private boolean tryCalculateGamma(int securityId, String optionType) {
//
//        // =========================================
//        // 1️⃣ GET STRIKE
//        // =========================================
//        Double strike = subscriptionRepository
//                .findStrikeBySecurityId(String.valueOf(securityId));
//
//        if (strike == null) {
//            selectedGammaMap.put(securityId, 0.0);
//            return false;
//        }
//
//        int strikeInt = strike.intValue();
//
//        int lowerStrike = strikeInt - 50;
//        int upperStrike = strikeInt + 50;
//
//        // =========================================
//        // 2️⃣ FIND NEIGHBOR SECURITY IDs
//        // =========================================
//        Integer lowerId = subscriptionRepository
//                .findSecurityIdByStrike(lowerStrike, optionType);
//
//        Integer upperId = subscriptionRepository
//                .findSecurityIdByStrike(upperStrike, optionType);
//
//        // ❗ EDGE CASE → missing neighbor → gamma = 0
//        if (lowerId == null || upperId == null) {
//            selectedGammaMap.put(securityId, 0.0);
//            return false;
//        }
//
//        // =========================================
//        // 3️⃣ FETCH DATA FROM 4-SEC MAP
//        // =========================================
//        OptionRsi current = fourthSecOptionMap.get(securityId);
//        OptionRsi lower   = fourthSecOptionMap.get(lowerId);
//        OptionRsi upper   = fourthSecOptionMap.get(upperId);
//
//        // ❗ If any missing → WAIT (do not assign 0 here)
//        if (current == null || lower == null || upper == null) {
//            return false;
//        }
//
//        // =========================================
//        // 4️⃣ SAME CANDLE VALIDATION (VERY IMPORTANT)
//        // =========================================
//        if (!current.getCandleTime().equals(lower.getCandleTime()) ||
//            !current.getCandleTime().equals(upper.getCandleTime())) {
//
//            return false; // wait for sync candle
//        }
//
//        // =========================================
//        // 5️⃣ GAMMA CALCULATION
//        // =========================================
//        double gamma;
//
//        if ("CALL".equalsIgnoreCase(optionType)) {
//            gamma = (lower.getClose() - upper.getClose()) / 100.0;
//        } else {
//            gamma = (upper.getClose() - lower.getClose()) / 100.0;
//        }
//
//        // =========================================
//        // 6️⃣ SANITY CHECK (OPTIONAL BUT STRONG)
//        // =========================================
//        if (Double.isNaN(gamma) || Double.isInfinite(gamma)) {
//            gamma = 0.0;
//        }
//
//        // Optional clamp (avoid crazy spikes)
//        if (Math.abs(gamma) > 10) {
//            gamma = 0.0;
//        }
//
//        // =========================================
//        // 7️⃣ ROUNDING (STABILITY)
//        // =========================================
//        gamma = Math.round(gamma * 1000.0) / 1000.0;
//
//        // =========================================
//        // 8️⃣ STORE FINAL GAMMA
//        // =========================================
//        selectedGammaMap.put(securityId, gamma);
//
//        // =========================================
//        // 9️⃣ CLEANUP PENDING
//        // =========================================
//        pendingGammaSet.remove(securityId);
//
//        return true;
//    }
//    
//    private void recomputePendingGamma() {
//
//        if (pendingGammaSet.isEmpty()) return;
//
//        Iterator<Integer> iterator = pendingGammaSet.iterator();
//
//        while (iterator.hasNext()) {
//
//            Integer secId = iterator.next();
//
//            OptionRsi rsi = fourthSecOptionMap.get(secId);
//
//            if (rsi == null) continue;
//
//            boolean success = tryCalculateGamma(secId, rsi.getOptionType());
//
//            if (success) {
//                iterator.remove(); // ✅ REMOVE ONLY WHEN SUCCESS
//                log.info("✅ Pending gamma resolved for {}", secId);
//            }
//        }
//    }
//    
//    // =========================================================
//    private OptionRsi process(String symbol,
//                             int securityId,
//                             String optionType,
//                             CandleSnapshot c,
//                             int oi,
//                             int highestOi,
//                             double atp) {
//
//        String key = symbol + "_" + securityId + "_5";
//
//        double ltp = c.close();
//
//        //  USE ONLY SELECTED GAMMA
//        double gamma = selectedGammaMap.getOrDefault(securityId, 0.0);
//
//        //if (gamma < 0.01) return null;
//
//        Integer prevOi = lastOiMap.getOrDefault(key, oi);
//        int deltaOi = oi - prevOi;
//
//        //  USE AVG LTP
//        double sign = (ltp > c.avgLtp()) ? 1 : -1;
//
//        double dpi = sign * Math.abs(deltaOi) * gamma * c.avgLtp();
//        double weightedOi = sign * Math.abs(deltaOi) * gamma;
//
//        lastOiMap.put(key, oi);
//
//        OptionRsi e = new OptionRsi();
//
//        e.setSymbol(symbol);
//        e.setSecurityId(securityId);
//        e.setOptionType(optionType);
//        e.setTimeframe("5S");
//
//        e.setCandleTime(
//                Instant.ofEpochSecond(c.startEpoch())
//                        .atZone(ZoneId.of("Asia/Kolkata"))
//                        .toLocalDateTime()
//        );
//
//        e.setOpen(c.open());
//        e.setHigh(c.high());
//        e.setLow(c.low());
//        e.setClose(c.close());
//
//        e.setOi(oi);
//        e.setHighestOi(highestOi);
//        e.setAtp(atp);
//
//        e.setDpi(dpi);
//        e.setWeightedOi(weightedOi);
//        e.setGamma(gamma);
//
//        latestMap.put(securityId, e);
//
//        return e;
//    }
//
//    public OptionRsi latest(int securityId) {
//        return latestMap.get(securityId);
//    }
//}

@Slf4j
@Component
public class CandleRsiService {

    private final OptionRsiRepository repo;
    private final OptionChainRepository optionChainRepository;
    private final DhanSubscriptionRepository subscriptionRepository;

    // ================= CANDLE =================
    private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();

    // ================= GAMMA =================
    private final Map<Integer, Double> selectedGammaMap = new ConcurrentHashMap<>();
    public final Map<Integer, OptionRsi> fourthSecOptionMap = new ConcurrentHashMap<>();
    private final Set<Integer> pendingGammaSet = ConcurrentHashMap.newKeySet();

    // ================= STRIKE CACHE (🔥 NO DB CALL) =================
    private final Map<Integer, Integer> strikeToSecurityCall = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> strikeToSecurityPut  = new ConcurrentHashMap<>();
    private final Map<Integer, DhanSubscription> securityToStrikeMap  = new ConcurrentHashMap<>();
    private final Map<LocalDate, Map<Integer, Integer>> expiryToStrikeToSecurityCall  = new ConcurrentHashMap<>();
    private final Map<LocalDate, Map<Integer, Integer>> expiryToStrikeToSecurityPut  = new ConcurrentHashMap<>();

    public final Map<Integer, OptionRsi> latestMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> isGammaPresent  = new ConcurrentHashMap<>();
    private final Map<String, Double> getGammaByKey = new ConcurrentHashMap<>();
    private final Map<String, Double> getPreviousLtpByKey = new ConcurrentHashMap<>();

    
    
    public CandleRsiService(OptionRsiRepository repo,
                            DhanSubscriptionRepository subscriptionRepository,
                            OptionChainRepository optionChainRepository) {
        this.repo = repo;
        this.subscriptionRepository = subscriptionRepository;
        this.optionChainRepository = optionChainRepository;
    }

    // =========================================================
    // 🔥 LOAD ALL STRIKES INTO MEMORY (ONE TIME)
    // =========================================================
    @PostConstruct
    public void loadSubscriptions() {

    	List<OptionChain> optionChainList = optionChainRepository.findLatestRecordForEachSecurityId();

    	
        List<DhanSubscription> list = subscriptionRepository.findByActiveTrue();

        list.parallelStream().forEach(s -> {
        	String key = "NIFTY" + "_" + s.getSecurityId() + "_5";
            int strike = (int) s.getStrike();
            int secId  = Integer.parseInt(s.getSecurityId());

            securityToStrikeMap.put(secId, s);
            
            isGammaPresent.put(key, false);

            if ("CALL".equalsIgnoreCase(s.getOptionType())) {

                strikeToSecurityCall.put(strike, secId);

                expiryToStrikeToSecurityCall
                        .computeIfAbsent(s.getExpiryDate(), k -> new HashMap<>())
                        .put(strike, secId);

            } else {

                strikeToSecurityPut.put(strike, secId);

                expiryToStrikeToSecurityPut
                        .computeIfAbsent(s.getExpiryDate(), k -> new HashMap<>())
                        .put(strike, secId);
            }
        });
        
        if (!optionChainList.isEmpty()) {
    		optionChainList.parallelStream().peek(op->{
    			String key = op.getSymbol() + "_" + op.getSecurityId() + "_5";
    			lastOiMap.put(key, op.getOi());
    			isGammaPresent.put(key, true);
    			getGammaByKey.put(key, op.getGamma());
    			getPreviousLtpByKey.put(key, op.getClose());
    		});
    		
    		
        }

        log.info("✅ Loaded {} subscriptions into memory", list.size());
    }

    // =========================================================
    public OptionRsi onLtp(String symbol, int securityId, String optionType,
                          double ltp, int oi, int highestOi,
                          double atp, int timeframeSeconds, LocalDate expiry) {

        long now = System.currentTimeMillis() / 1000;
        String key = symbol + "_" + securityId + "_" + timeframeSeconds;
        String key1 = symbol + "_" + securityId + "_5" ;

        CandleState candle = candleMap.computeIfAbsent(
                key, k -> new CandleState(timeframeSeconds)
        );

//        candle.onTick(ltp, now);
//
//        if (!candle.isComplete(now)) return null;
//
//        CandleSnapshot snap = candle.snapshotAndReset(now, ltp);
        
        CandleSnapshot snap = candle.onTick(ltp, now);

        if (snap == null) return null;

        // =====================================================
        // 🔥 4 SEC → GAMMA ENGINE
        // =====================================================
        if ((isGammaPresent.get(key1)!=null && !isGammaPresent.get(key1)) && !optionType.equalsIgnoreCase("FUTURE") && timeframeSeconds == 4) {

            OptionRsi rsi4 = new OptionRsi();
            rsi4.setSecurityId(securityId);
            rsi4.setOptionType(optionType);
            rsi4.setClose(snap.close());

            rsi4.setCandleTime(
                Instant.ofEpochSecond(snap.startEpoch())
                    .atZone(ZoneId.of("Asia/Kolkata"))
                    .toLocalDateTime()
            );

            fourthSecOptionMap.put(securityId, rsi4);

            boolean success = tryCalculateGamma(securityId, optionType);

            if (!success) {
                pendingGammaSet.add(securityId);
            }

            // 🔥 RECOMPUTE PENDING
            recomputePendingGamma();

            // 🔥 MEMORY SAFETY
            if (fourthSecOptionMap.size() > 2000) {
                fourthSecOptionMap.clear();
                pendingGammaSet.clear();
                log.warn("⚠ Cleared 4-sec cache (memory protection)");
            }

            return null;
        }

        // =====================================================
        // 5 SEC FLOW
        // =====================================================
        return process(symbol, securityId, optionType,
                snap, oi, highestOi, atp, expiry);
    }

    // =========================================================
    // 🔥 FAST GAMMA CALCULATION (NO DB)
    // =========================================================
    private boolean tryCalculateGamma(int securityId, String optionType) {

    	DhanSubscription ds = securityToStrikeMap.get(securityId);
    	Integer strikeInt = ds.getStrike();
        if (strikeInt == null) return false;

        int lowerStrike = strikeInt - 50;
        int upperStrike = strikeInt + 50;

        Integer lowerId = "CALL".equalsIgnoreCase(optionType)
                ? expiryToStrikeToSecurityCall.get(ds.getExpiryDate()).get(lowerStrike)
                : expiryToStrikeToSecurityPut.get(ds.getExpiryDate()).get(lowerStrike);
        
        Integer upperId = "CALL".equalsIgnoreCase(optionType)
        		? expiryToStrikeToSecurityCall.get(ds.getExpiryDate()).get(upperStrike)
                : expiryToStrikeToSecurityPut.get(ds.getExpiryDate()).get(upperStrike);

        // EDGE → gamma = 0
        if (lowerId == null || upperId == null) {
            selectedGammaMap.put(securityId, 0.0);
            return false;
        }

        OptionRsi current = fourthSecOptionMap.get(securityId);
        OptionRsi lower   = fourthSecOptionMap.get(lowerId);
        OptionRsi upper   = fourthSecOptionMap.get(upperId);

        if (current == null || lower == null || upper == null) {
            return false;
        }

        if (!current.getCandleTime().equals(lower.getCandleTime()) ||
            !current.getCandleTime().equals(upper.getCandleTime())) {
            return false;
        }

        double gamma;

        if ("CALL".equalsIgnoreCase(optionType)) {
            gamma = Math.abs(lower.getClose() - upper.getClose()) / 100.0;
        } else {
            gamma = Math.abs(upper.getClose() - lower.getClose()) / 100.0;
        }

        if (Double.isNaN(gamma) || Double.isInfinite(gamma)) {
            gamma = 0.0;
        }

        if (Math.abs(gamma) > 10 || Math.abs(gamma) < 0.01) {
            gamma = 0.0;
        }

        gamma = Math.round(gamma * 1000.0) / 1000.0;

        selectedGammaMap.put(securityId, gamma);

        return true;
    }

    // =========================================================
    // 🔁 RECOMPUTE DELAYED STRIKES
    // =========================================================
    private void recomputePendingGamma() {

        if (pendingGammaSet.isEmpty()) return;

        Iterator<Integer> iterator = pendingGammaSet.iterator();

        while (iterator.hasNext()) {

            Integer secId = iterator.next();

            OptionRsi rsi = fourthSecOptionMap.get(secId);
            if (rsi == null) continue;

            boolean success = tryCalculateGamma(secId, rsi.getOptionType());

            if (success) {
                iterator.remove();
                log.debug("✅ Gamma resolved for {}", secId);
            }
        }
    }

    // =========================================================
    // 5 SEC PROCESSING
    // =========================================================
    private OptionRsi process(String symbol,
                             int securityId,
                             String optionType,
                             CandleSnapshot c,
                             int oi,
                             int highestOi,
                             double atp, LocalDate expiry) {

        String key = symbol + "_" + securityId + "_5";

        double gamma = "FUTURE".equalsIgnoreCase(optionType) ? 0.0678 : (isGammaPresent.get(key)!=null && isGammaPresent.get(key)) ? getGammaByKey.get(key) : selectedGammaMap.getOrDefault(securityId, 0.0);

        gamma = gamma > 1 ? 1 : gamma;
        
        Integer prevOi = lastOiMap.getOrDefault(key, oi);
        int deltaOi = oi - prevOi;
        
        double prevLtp = c.avgLtp();
        if (isGammaPresent.get(key)!=null && isGammaPresent.get(key)) {
        	prevLtp = getPreviousLtpByKey.get(key);
        }

        double sign = (c.close() > prevLtp) ? 1 : -1;

        double dpi = sign * Math.abs(deltaOi) * gamma * prevLtp;
        double weightedOi = sign * Math.abs(deltaOi) * gamma;

        lastOiMap.put(key, oi);

        OptionRsi e = new OptionRsi();

        e.setSymbol(symbol);
        e.setSecurityId(securityId);
        e.setOptionType(optionType);
        e.setTimeframe("5S");

        e.setCandleTime(
                Instant.ofEpochSecond(c.startEpoch())
                        .atZone(ZoneId.of("Asia/Kolkata"))
                        .toLocalDateTime()
        );

        e.setOpen(c.open());
        e.setHigh(c.high());
        e.setLow(c.low());
        e.setClose(c.close());

        e.setOi(oi);
        e.setHighestOi(highestOi);
        e.setAtp(atp);

        e.setDpi(dpi);
        e.setWeightedOi(weightedOi);
        e.setGamma(gamma);
        e.setExpiry(expiry);

        latestMap.put(securityId, e);
        
        OptionChain optionChain = OptionChain.builder()
        .id(e.getSymbol().toUpperCase() + e.getSecurityId()+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")))
        .symbol(e.getSymbol())
        .securityId(e.getSecurityId())
        .optionType(e.getOptionType())
        .open(e.getOpen())
        .low(e.getLow())
        .high(e.getHigh())
        .close(e.getClose())
        .oi(e.getOi())
        .highestOi(e.getHighestOi())
        .atp(e.getAtp())
        .dpi(e.getDpi())
        .gamma(e.getGamma())
        .expiry(e.getExpiry())
        .candleTime(e.getCandleTime())
        .build();
        
        log.info("Option Chain:: {}", optionChain);
        
        //LocalDateTime today328PM = LocalDate.now().atTime(15, 28);

        //if (!optionChain.getCandleTime().isBefore(today328PM)) {
            optionChainRepository.save(optionChain);  //here optionChain save should be parralel or asyncronous because of every 5 sec every security id's record save so time cost may be increased
        //}
        
        isGammaPresent.put(key, false);

        return e;
    }

    public OptionRsi latest(int securityId) {
        return latestMap.get(securityId);
    }
}