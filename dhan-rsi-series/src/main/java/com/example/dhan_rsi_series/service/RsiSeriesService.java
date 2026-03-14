package com.example.dhan_rsi_series.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.mapper.CandleMapper;
import com.example.dhan_rsi_series.model.CandleData;
import com.example.dhan_rsi_series.model.HistoricalCandleRequest;
import com.example.dhan_rsi_series.model.RsiCandle;
import com.example.dhan_rsi_series.utils.RsiExcelGenerator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RsiSeriesService {

    private final DhanMarketDataService marketDataService;

    public Mono<List<RsiCandle>> calculateRsiSeries(
            String securityId, int interval, int period) {

        HistoricalCandleRequest request = HistoricalCandleRequest.builder()
                .securityId(securityId)
                .exchangeSegment("NSE_FNO")
                .instrument("OPTIDX")
                .interval(String.valueOf(interval))
                .oi(true)
                .fromDate("2026-01-02 09:15:00")
                .toDate("2026-01-02 13:30:00")
                .build();

        return marketDataService.fetchIntradayCandles(request)
                .map(CandleMapper::toCandleDataList)
                .map(candleDataList -> calculateRsi(candleDataList, period));
    }
    
    public Mono<List<RsiCandle>> calculateRsiSeries(HistoricalCandleRequest request) {
        return marketDataService.fetchIntradayCandles(request)
                .map(CandleMapper::toCandleDataList)
                .map(candleDataList -> calculateRsi(candleDataList, 14));
    }

    /* ===== PURE JAVA RSI ===== */
    private List<RsiCandle> calculateRsi(
            List<CandleData> candleDataList, int period) {

        if (candleDataList.size() <= period) {
            throw new IllegalArgumentException("Not enough candles for RSI");
        }

        List<RsiCandle> result = new ArrayList<>();

        double gain = 0, loss = 0;

        double cumulativePV = 0;
        long cumulativeVolume = 0;

        // ================= INITIAL RSI =================
        for (int i = 1; i <= period; i++) {
            double diff = candleDataList.get(i).getClose()
                    - candleDataList.get(i - 1).getClose();
            if (diff > 0) gain += diff;
            else loss += Math.abs(diff);
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        double rsi = avgLoss == 0 ? 100 :
                100 - (100 / (1 + avgGain / avgLoss));

        // ================= FIRST CANDLE =================
        CandleData cd = candleDataList.get(period);

        double typicalPrice = (cd.getHigh() + cd.getLow() + cd.getClose()) / 3;
        cumulativePV += typicalPrice * cd.getVolume();
        cumulativeVolume += cd.getVolume();

        double vwap = cumulativePV / cumulativeVolume;

        result.add(RsiExcelGenerator.buildRsiCandle(cd, rsi, vwap));

        // ================= REST CANDLES =================
        for (int i = period + 1; i < candleDataList.size(); i++) {

            CandleData current = candleDataList.get(i);
            CandleData previous = candleDataList.get(i - 1);

            double diff = current.getClose() - previous.getClose();

            double currentGain = Math.max(diff, 0);
            double currentLoss = Math.max(-diff, 0);

            avgGain = ((avgGain * (period - 1)) + currentGain) / period;
            avgLoss = ((avgLoss * (period - 1)) + currentLoss) / period;

            rsi = avgLoss == 0 ? 100 :
                    100 - (100 / (1 + avgGain / avgLoss));

            typicalPrice =
                    (current.getHigh() + current.getLow() + current.getClose()) / 3;

            cumulativePV += typicalPrice * current.getVolume();
            cumulativeVolume += current.getVolume();

            vwap = cumulativePV / cumulativeVolume;

            result.add(RsiExcelGenerator.buildRsiCandle(current, rsi, vwap));
        }

        return result;
    }

}
