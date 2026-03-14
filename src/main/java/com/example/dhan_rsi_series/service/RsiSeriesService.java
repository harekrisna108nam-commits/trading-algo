package com.example.dhan_rsi_series.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.mapper.CandleMapper;
import com.example.dhan_rsi_series.model.CandleData;
import com.example.dhan_rsi_series.model.HistoricalCandleRequest;
import com.example.dhan_rsi_series.model.RsiCandle;
import com.example.dhan_rsi_series.utils.EmaSmoothedRsiCalculator;
import com.example.dhan_rsi_series.utils.RsiExcelGenerator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RsiSeriesService {

	private final DhanMarketDataService marketDataService;

	public Mono<List<RsiCandle>> calculateRsiSeries(String securityId, int interval, int period) {

		HistoricalCandleRequest request = HistoricalCandleRequest.builder().securityId(securityId)
				.exchangeSegment("NSE_FNO").instrument("OPTIDX").interval(String.valueOf(interval)).oi(true)
				.fromDate("2026-01-02 09:15:00").toDate("2026-01-02 13:30:00").build();

		return marketDataService.fetchIntradayCandles(request).map(CandleMapper::toCandleDataList)
				.map(candleDataList -> calculateRsi(candleDataList, period));
	}

	public Mono<List<RsiCandle>> calculateRsiSeries(HistoricalCandleRequest request) {
		return marketDataService.fetchIntradayCandles(request).map(CandleMapper::toCandleDataList)
				.map(candleDataList -> calculateRsi(candleDataList, 14));
	}
	
	public void calculateRsiSeries(List<HistoricalCandleRequest> requestList) {
		
		List<List<CandleData>> collectList = Flux.fromIterable(requestList)
        .flatMap(req -> marketDataService.fetchIntradayCandles(req)
                .map(CandleMapper::toCandleDataList))
        .collectList().block();
		
		
	}

	/* ===== PURE JAVA RSI ===== */
	/*
	 * private List<RsiCandle> calculateRsi( List<CandleData> candleDataList, int
	 * period) {
	 * 
	 * if (candleDataList.size() <= period) { throw new
	 * IllegalArgumentException("Not enough candles for RSI"); }
	 * 
	 * List<RsiCandle> result = new ArrayList<>();
	 * 
	 * double gain = 0, loss = 0;
	 * 
	 * double cumulativePV = 0; long cumulativeVolume = 0;
	 * 
	 * // ================= INITIAL RSI ================= for (int i = 1; i <=
	 * period; i++) { double diff = candleDataList.get(i).getClose() -
	 * candleDataList.get(i - 1).getClose(); if (diff > 0) gain += diff; else loss
	 * += Math.abs(diff); }
	 * 
	 * double avgGain = gain / period; double avgLoss = loss / period;
	 * 
	 * double rsi = avgLoss == 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss));
	 * 
	 * // ================= FIRST CANDLE ================= CandleData cd =
	 * candleDataList.get(period);
	 * 
	 * double typicalPrice = (cd.getHigh() + cd.getLow() + cd.getClose()) / 3;
	 * cumulativePV += typicalPrice * cd.getVolume(); cumulativeVolume +=
	 * cd.getVolume();
	 * 
	 * double vwap = cumulativePV / cumulativeVolume;
	 * 
	 * result.add(RsiExcelGenerator.buildRsiCandle(cd, rsi, vwap));
	 * 
	 * // ================= REST CANDLES ================= for (int i = period + 1;
	 * i < candleDataList.size(); i++) {
	 * 
	 * CandleData current = candleDataList.get(i); CandleData previous =
	 * candleDataList.get(i - 1);
	 * 
	 * double diff = current.getClose() - previous.getClose();
	 * 
	 * double currentGain = Math.max(diff, 0); double currentLoss = Math.max(-diff,
	 * 0);
	 * 
	 * avgGain = ((avgGain * (period - 1)) + currentGain) / period; avgLoss =
	 * ((avgLoss * (period - 1)) + currentLoss) / period;
	 * 
	 * rsi = avgLoss == 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss));
	 * 
	 * typicalPrice = (current.getHigh() + current.getLow() + current.getClose()) /
	 * 3;
	 * 
	 * cumulativePV += typicalPrice * current.getVolume(); cumulativeVolume +=
	 * current.getVolume();
	 * 
	 * vwap = cumulativePV / cumulativeVolume;
	 * 
	 * result.add(RsiExcelGenerator.buildRsiCandle(current, rsi, vwap)); }
	 * 
	 * return result; }
	 */

	private List<RsiCandle> calculateRsi(List<CandleData> candles, int period) {

		if (candles.size() <= period) {
			throw new IllegalArgumentException("Not enough candles for RSI");
		}

		List<RsiCandle> result = new ArrayList<>();

		EmaSmoothedRsiCalculator rsiCalc = new EmaSmoothedRsiCalculator(period, 5);

		double cumulativePV = 0;
		long cumulativeVolume = 0;

		for (CandleData cd : candles) {

			// ===== RSI =====
			double rsi = rsiCalc.update(cd.getClose());
			double highRsi = rsiCalc.update(cd.getHigh());
			double lowRsi = rsiCalc.update(cd.getLow());

			// skip until first RSI ready
			// if (rsi == -1) continue;

			if (rsi == -1 || highRsi == -1 || lowRsi == -1) {

				continue;
			} else {
				double rsiBuffer[] = new double[3];
				rsiBuffer[0] = rsiBuffer[1];
				rsiBuffer[1] = rsiBuffer[2];
				rsiBuffer[2] = rsi;
				if (rsiBuffer[0] > 0 && rsiBuffer[1] > 0 && rsiBuffer[2] > 0) {
					rsi = (rsiBuffer[0] + rsiBuffer[1] + rsiBuffer[2]) / 3.0;
				}

				double highRsiBuffer[] = new double[3];
				highRsiBuffer[0] = highRsiBuffer[1];
				highRsiBuffer[1] = highRsiBuffer[2];
				highRsiBuffer[2] = rsi;
				if (highRsiBuffer[0] > 0 && highRsiBuffer[1] > 0 && highRsiBuffer[2] > 0) {
					highRsi = (highRsiBuffer[0] + highRsiBuffer[1] + highRsiBuffer[2]) / 3.0;
				}

				double lowRsiBuffer[] = new double[3];
				lowRsiBuffer[0] = lowRsiBuffer[1];
				lowRsiBuffer[1] = lowRsiBuffer[2];
				lowRsiBuffer[2] = rsi;
				if (lowRsiBuffer[0] > 0 && lowRsiBuffer[1] > 0 && lowRsiBuffer[2] > 0) {
					lowRsi = (lowRsiBuffer[0] + lowRsiBuffer[1] + lowRsiBuffer[2]) / 3.0;
				}

			}

			// ===== VWAP =====
			double typicalPrice = (cd.getHigh() + cd.getLow() + cd.getClose()) / 3;

			cumulativePV += typicalPrice * cd.getVolume();
			cumulativeVolume += cd.getVolume();

			double vwap = cumulativePV / cumulativeVolume;

			// ===== Build result =====
			result.add(RsiExcelGenerator.buildRsiCandle(cd, rsi, vwap, highRsi, lowRsi));
		}

		return result;
	}

}
