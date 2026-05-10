package com.example.dhan_rsi_series.utils;

import java.util.ArrayDeque;
import java.util.Deque;

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

//@Getter
//@Setter
//public class CandleState {
//
//    private final int timeframeSeconds;
//
//    private long startEpoch;
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
//        // start new candle
//        if (!started) {
//            startEpoch = nowEpoch;
//            open = high = low = close = price;
//            started = true;
//            return;
//        }
//
//        // update current candle
//        high = Math.max(high, price);
//        low = Math.min(low, price);
//        close = price;
//        
//    }
//
//    public boolean isComplete(long nowEpoch) {
//        return started && (nowEpoch - startEpoch) >= timeframeSeconds;
//    }
//
//    // ✅ CONTINUOUS CANDLE (NO GAP FIX)
//    public CandleSnapshot snapshotAndReset(long nowEpoch, double lastPrice) {
//
//        CandleSnapshot snap = new CandleSnapshot(
//                open, high, low, close, startEpoch   // ✅ FIXED TIME
//        );
//
//        // ✅ Immediately start next candle (CRITICAL FIX)
//        startEpoch = nowEpoch;
//        open = high = low = close = lastPrice;
//        started = true;
//
//        return snap;
//    }
//}

//@Getter
//@Setter
//public class CandleState {
//
//	private final int timeframeSeconds;
//
//	private long startEpoch;
//	private long currentSecond = -1;
//
//	private double open;
//	private double high;
//	private double low;
//	private double close;
//
//	private boolean started = false;
//
//	// store last price of each second
//	private final Deque<Double> perSecondLtp = new ArrayDeque<>();
//
//	public CandleState(int timeframeSeconds) {
//		this.timeframeSeconds = timeframeSeconds;
//	}
//
//	public void onTick(double price, long nowEpoch) {
//
//		long sec = nowEpoch;
//
//		// ================= START NEW CANDLE =================
//		if (!started) {
//			startEpoch = sec;
//			currentSecond = sec;
//
//			open = high = low = close = price;
//			started = true;
//
//			return;
//		}
//
//		// ================= UPDATE OHLC =================
//		high = Math.max(high, price);
//		low = Math.min(low, price);
//		close = price;
//
//		// ================= PER SECOND LOGIC =================
//		if (sec != currentSecond) {
//			// new second → store last second close
//			if (timeframeSeconds == 5) {
//				perSecondLtp.addLast(close);
//			}
//
//			if (perSecondLtp.size() > timeframeSeconds) {
//				perSecondLtp.pollFirst();
//			}
//
//			currentSecond = sec;
//		}
//	}
//
//	public boolean isComplete(long nowEpoch) {
//		return started && (nowEpoch - startEpoch) >= timeframeSeconds;
//	}
//
//	// RETURN AVG WITH SNAPSHOT
//	public CandleSnapshot snapshotAndReset(long nowEpoch, double lastPrice) {
//
//    	double avgLtp = 0.0;
//    	if (timeframeSeconds==5) {
//    		avgLtp = perSecondLtp.stream()
//                    .mapToDouble(Double::doubleValue)
//                    .average()
//                    .orElse(lastPrice);
//    	}
//        
//
//        CandleSnapshot snap = new CandleSnapshot(
//                open, high, low, close, startEpoch, avgLtp // IMPORTANT
//        );
//
//        // ================= RESET =================
//        startEpoch = nowEpoch;
//        currentSecond = nowEpoch;
//
//        open = high = low = close = lastPrice;
//        started = true;
//
//        perSecondLtp.clear(); // reset for next candle
//
//        return snap;
//    }
//}

@Getter
@Setter
public class CandleState {

    private final int timeframeSeconds;

    private long startEpoch;
    private long currentSecond = -1;

    private double open;
    private double high;
    private double low;
    private double close;

    private boolean started = false;

    private final Deque<Double> perSecondLtp = new ArrayDeque<>();

    public CandleState(int timeframeSeconds) {
        this.timeframeSeconds = timeframeSeconds;
    }

    // 🔥 SINGLE ENTRY POINT (thread-safe)
    public synchronized CandleSnapshot onTick(double price, long nowEpoch) {

        long sec = nowEpoch;
        long alignedStart = (sec / timeframeSeconds) * timeframeSeconds;

        // ================= START =================
        if (!started) {
            startEpoch = alignedStart;
            currentSecond = sec;

            open = high = low = close = price;
            started = true;
            return null;
        }

        // ================= NEW CANDLE =================
        if (alignedStart != startEpoch) {

            CandleSnapshot snap = buildSnapshot();

            // RESET FOR NEXT CANDLE
            startEpoch = alignedStart;
            currentSecond = sec;

            open = high = low = close = price;
            perSecondLtp.clear();

            return snap; // ✅ ONLY place snapshot is emitted
        }

        // ================= UPDATE =================
        high = Math.max(high, price);
        low = Math.min(low, price);
        close = price;

        if (sec != currentSecond) {
            if (timeframeSeconds == 5) {
                perSecondLtp.addLast(close);
            }

            if (perSecondLtp.size() > timeframeSeconds) {
                perSecondLtp.pollFirst();
            }

            currentSecond = sec;
        }

        return null;
    }

    private CandleSnapshot buildSnapshot() {

        double avgLtp = 0.0;

        if (timeframeSeconds == 5) {
            avgLtp = perSecondLtp.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(close);
        }

        return new CandleSnapshot(
                open,
                high,
                low,
                close,
                startEpoch, // 🔥 FIXED TIME BUCKET
                avgLtp
        );
    }
}