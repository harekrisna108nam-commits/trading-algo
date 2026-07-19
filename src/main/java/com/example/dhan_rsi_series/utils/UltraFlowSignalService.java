package com.example.dhan_rsi_series.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.enums.FlowSignal;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UltraFlowSignalService {

	private final Map<String, Boolean> firstTimeExecution = new ConcurrentHashMap<>();
	public final Map<String, OptionRsi> callBaseBucket = new ConcurrentHashMap<>();
	public final Map<String, OptionRsi> putBaseBucket = new ConcurrentHashMap<>();
	private final String callKey = "NIFTY" + "_" + "57344" + "_5";
	private final String putKey = "NIFTY" + "_" + "57345" + "_5";

	// =========================================================
	// 🔥 LOAD (ONE TIME)
	// =========================================================
	@PostConstruct
	public void load() {
		firstTimeExecution.put(callKey, true);
		firstTimeExecution.put(putKey, true);
	}

	public FlowSignal evaluate(DpiAggregatorService.Snapshot snap, OptionRsi e, OptionRsi optionRsiPrev,
			String optionType, boolean callBuy, boolean putBuy) {

		// =====================================================================
		// ---------------------------------CALL--------------------------------
		// =====================================================================

		if (e.getOptionType().equalsIgnoreCase("CALL")) {

			// 0. First time set the value
			if (firstTimeExecution.getOrDefault(callKey, true)) {
				callBaseBucket.put(callKey, e);

				firstTimeExecution.put(callKey, false);
				// callBuyingBucket.put(callKey, e);
				return FlowSignal.HOLD;
			}

			firstTimeExecution.put(callKey, false);

			// 1. Call Base line Bucket replacement

			boolean callCloseCondition = (callBaseBucket.getOrDefault(callKey, e).getClose() <= e.getClose());

			if (callCloseCondition) {
				callBaseBucket.put(callKey, e);
				return FlowSignal.HOLD;
			}

			Double callBucketNetFlow = callBaseBucket.getOrDefault(callKey, e).getNetFlow();
			Double callBucketCallFlow = callBaseBucket.getOrDefault(callKey, e).getCallFlow();

			Double currentCallNetFlow = e.getNetFlow();
			Double currentCallCallFlow = e.getCallFlow();

			if (currentCallNetFlow < callBucketNetFlow & currentCallCallFlow < callBucketCallFlow) {
				return FlowSignal.BUY_PUT;
			}
			
//			if (currentCallNetFlow > callBucketNetFlow & currentCallCallFlow >= callBucketCallFlow) {
//				return FlowSignal.SELL_PUT;
//			}
			
		}

		if (e.getOptionType().equalsIgnoreCase("PUT")) {

			// 0. First time set the value
			if (firstTimeExecution.getOrDefault(putKey, true)) {
				putBaseBucket.put(putKey, e);
				firstTimeExecution.put(putKey, false);
				return FlowSignal.HOLD;
			}

			firstTimeExecution.put(putKey, false);

			boolean putCloseCondition = (putBaseBucket.getOrDefault(putKey, e).getClose() <= e.getClose());

			if (putCloseCondition) {
				putBaseBucket.put(putKey, e);
				return FlowSignal.HOLD;
			}

			Double putBucketNetFlow = putBaseBucket.getOrDefault(putKey, e).getNetFlow();
			Double putBucketPutFlow = putBaseBucket.getOrDefault(putKey, e).getPutFlow();

			Double currentPutNetFlow = e.getNetFlow();
			Double currentPutPutFlow = e.getPutFlow();

			if (currentPutNetFlow > putBucketNetFlow & currentPutPutFlow < putBucketPutFlow) {
				return FlowSignal.BUY_CALL;
			}		

//			if (currentPutNetFlow < putBucketNetFlow & currentPutCallFlow >= putBucketPutFlow) {
//				return FlowSignal.SELL_CALL;
//			}

		}

		return FlowSignal.HOLD;
	}
}