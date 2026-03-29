package com.example.dhan_rsi_series.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.repository.OptionRsiRepository;
import com.example.dhan_rsi_series.utils.CandleSnapshot;
import com.example.dhan_rsi_series.utils.CandleState;
import com.example.dhan_rsi_series.utils.EmaSmoothedRsiCalculator;

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

@Component
public class CandleRsiService {

    private final OptionRsiRepository repo;

    private final Map<String, CandleState> candleMap = new ConcurrentHashMap<>();

    private final Map<String, Integer> lastOiMap = new ConcurrentHashMap<>();
    private final Map<String, Double> lastLtpMap = new ConcurrentHashMap<>();
    private final Map<String, Double> lastDeltaLtpMap = new ConcurrentHashMap<>();

    // warmup counter (IMPORTANT)
    private final Map<String, Integer> warmupCountMap = new ConcurrentHashMap<>();

    // latest per securityId
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

        CandleSnapshot snap = candle.snapshotAndReset();

        // ===== Warmup Logic (3 candles) =====
        int count = warmupCountMap.getOrDefault(key, 0) + 1;
        warmupCountMap.put(key, count);

        if (count < 3) {
            updateMemory(key, snap.close(), oi);
            return null;
        }

        // ===== After warmup =====
        return save(symbol, securityId, optionType, timeframeSeconds,
                snap, oi, highestOi, atp);
    }

    // =========================================================

    private void updateMemory(String key, double ltp, int oi) {
        lastLtpMap.put(key, ltp);
        lastOiMap.put(key, oi);
        lastDeltaLtpMap.put(key, 0.0);
    }

    // =========================================================

    private OptionRsi save(String symbol, int securityId, String optionType,
                           int timeframe, CandleSnapshot c,
                           int oi, int highestOi, double atp) {

        double ltp = c.close();
        String key = symbol + "_" + securityId + "_" + timeframe;

        // ===== previous values =====
        int prevOi = lastOiMap.getOrDefault(key, oi);
        double prevLtp = lastLtpMap.getOrDefault(key, ltp);

        // ===== deltas =====
        int deltaOi = Math.abs(oi - prevOi);
        double deltaLtp = ltp - prevLtp;

        double prevDeltaLtp = lastDeltaLtpMap.getOrDefault(key, deltaLtp);

        // second derivative (acceleration)
        double deltaDeltaLtp = deltaLtp - prevDeltaLtp;

        double dpi = deltaOi * deltaLtp * ltp;

        // ===== update memory =====
        lastOiMap.put(key, oi);
        lastLtpMap.put(key, ltp);
        lastDeltaLtpMap.put(key, deltaLtp);

        // ===== persist =====
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

        e.setDpi(dpi);
        e.setDeltaLtp(deltaLtp);
        e.setDeltaDeltaLtp(deltaDeltaLtp);

        // simple signals
        e.setBuy(deltaDeltaLtp > 0);
        e.setSell(deltaDeltaLtp < 0);

        // store latest
        latestMap.put(securityId, e);

        // optional DB save
        // repo.save(e);

        System.out.printf(
                "%s %s LTP=%.2f ΔLTP=%.2f ΔΔLTP=%.2f DPI=%.2f OI=%d%n",
                optionType,
                e.getTimeframe(),
                ltp,
                deltaLtp,
                deltaDeltaLtp,
                dpi,
                oi
        );

        return e;
    }

    // =========================================================

    private String toFrame(int tf) {
        return switch (tf) {
            case 5 -> "5S";
            case 30 -> "30S";
            case 60 -> "1M";
            default -> tf + "S";
        };
    }

    // =========================================================

    public OptionRsi latest(int securityId) {
        return latestMap.get(securityId);
    }
}