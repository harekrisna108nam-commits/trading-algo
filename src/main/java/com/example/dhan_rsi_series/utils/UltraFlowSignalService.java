package com.example.dhan_rsi_series.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.enums.FlowSignal;
import com.example.dhan_rsi_series.model.OptionFlow;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UltraFlowSignalService {

	private final Map<String, Boolean> firstTimeExecution = new ConcurrentHashMap<>();
	public final Map<String, OptionRsi> callBaseBucket = new ConcurrentHashMap<>();
	public final Map<String, OptionRsi> putBaseBucket = new ConcurrentHashMap<>();
	public static FlowSignal marketType = FlowSignal.HOLD;
	//public final Map<String, OptionRsi> referenceBucket = new ConcurrentHashMap<>();
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

	public OptionFlow evaluate(DpiAggregatorService.Snapshot snap, OptionRsi e, OptionRsi optionRsiPrev,
			String optionType, boolean callBuy, boolean putBuy, Map<String, OptionRsi> referenceBucket) {

		// =====================================================================
		// ---------------------------------CALL--------------------------------
		// =====================================================================

		if (e.getOptionType().equalsIgnoreCase("CALL")) {

			// 0. First time set the value
			if (firstTimeExecution.getOrDefault(callKey, true)) {
				callBaseBucket.put(callKey, e);
				firstTimeExecution.put(callKey, false);
				
//				e.setCallBucketClose(e.getClose());
//				e.setCallBucketCallFlow(e.getCallFlow());
//				e.setCallBucketPutFlow(e.getPutFlow());
//				e.setCallBucketNetFlow(e.getNetFlow());
				
				// callBuyingBucket.put(callKey, e);
				return OptionFlow.builder()
						.option(e)
						.flow(FlowSignal.HOLD)
						.build();
			}

			firstTimeExecution.put(callKey, false);
			
			// 1. Call Base line Bucket replacement

			boolean callCloseCondition = (callBaseBucket.getOrDefault(callKey, e).getClose() <= e.getClose());
			
			if (callCloseCondition) {
				//OptionRsi callOption = callBaseBucket.getOrDefault(callKey, e);
				//OptionRsi putOption = putBaseBucket.getOrDefault(putKey, e);
				//callOption.setClose(e.getClose());
				//callBaseBucket.put(callKey, callOption);
				
//				e.setCallBucketClose(callOption.getClose());
//				e.setCallBucketCallFlow(callOption.getCallFlow());
//				e.setCallBucketPutFlow(callOption.getPutFlow());
//				e.setCallBucketNetFlow(callOption.getNetFlow());
				
				
				OptionRsi callOption = callBaseBucket.getOrDefault(callKey, e);
//				OptionRsi refOption  = referenceBucket.getOrDefault(callKey, e);
//
//				// Individual bucket values
//				double callBucketCallFlow = callOption.getCallFlow();
//				double callBucketPutFlow  = callOption.getPutFlow();
//				double callBucketNetFlow  = callOption.getNetFlow();
//
//				double refBucketCallFlow = refOption.getCallFlow();
//				double refBucketPutFlow  = refOption.getPutFlow();
//				double refBucketNetFlow  = refOption.getNetFlow();
//
//				// Average values
//				double avgCallFlow = (callBucketCallFlow + refBucketCallFlow) / 2.0;
//				double avgPutFlow  = (callBucketPutFlow  + refBucketPutFlow) / 2.0;
//				double avgNetFlow  = (callBucketNetFlow  + refBucketNetFlow) / 2.0;
//				
//				refOption.setClose(e.getClose());
//				refOption.setCallFlow(avgCallFlow);
//				refOption.setPutFlow(avgPutFlow);
//				refOption.setNetFlow(avgNetFlow);
				
				callOption.setClose(e.getClose());
				//update the call base bucket
				callBaseBucket.put(callKey, callOption);
				
				return OptionFlow.builder()
						.option(e)
						.flow(FlowSignal.HOLD)
						.build();
			}

			Double callBucketNetFlow = callBaseBucket.getOrDefault(callKey, e).getNetFlow();
			Double callBucketCallFlow = callBaseBucket.getOrDefault(callKey, e).getCallFlow();

			Double currentCallNetFlow = e.getNetFlow();
			Double currentCallCallFlow = e.getCallFlow();

			if (currentCallNetFlow < callBucketNetFlow & currentCallCallFlow < callBucketCallFlow) {
				
				
				OptionRsi putOption = putBaseBucket.getOrDefault(putKey, e);
				OptionRsi callOption = callBaseBucket.getOrDefault(callKey, e);
				OptionRsi refOption = referenceBucket.getOrDefault(putKey, e);

				// Individual bucket values
				double putBucketCallFlow = putOption.getCallFlow();
				double putBucketPutFlow  = putOption.getPutFlow();
				double putBucketNetFlow  = putOption.getNetFlow();

				double refBucketCallFlow = refOption.getCallFlow();
				double refBucketPutFlow  = refOption.getPutFlow();
				double refBucketNetFlow  = refOption.getNetFlow();

				// Average values
				double avgCallFlow = (putBucketCallFlow + refBucketCallFlow) / 2.0;
				double avgPutFlow  = (putBucketPutFlow  + refBucketPutFlow) / 2.0;
				double avgNetFlow  = (putBucketNetFlow  + refBucketNetFlow) / 2.0;
				
				//refOption.setClose(e.getClose());
				putOption.setCallFlow(avgCallFlow);
				putOption.setPutFlow(avgPutFlow);
				putOption.setNetFlow(avgNetFlow);
				
				callOption.setCallFlow(avgCallFlow);
				callOption.setPutFlow(avgPutFlow);
				callOption.setNetFlow(avgNetFlow);
				
				//update the put base bucket
				putBaseBucket.put(putKey, putOption);
				
				callBaseBucket.put(callKey, callOption);
				
//				e.setPutBucketClose(refOption.getClose());
//				e.setPutBucketCallFlow(refOption.getCallFlow());
//				e.setPutBucketPutFlow(refOption.getPutFlow());
//				e.setPutBucketNetFlow(refOption.getNetFlow());
				
				marketType = FlowSignal.SELL_CALL;
				
				return OptionFlow.builder()
						.option(e)
						.flow(FlowSignal.BUY_PUT)
						.build();
			}
			
		}

		if (e.getOptionType().equalsIgnoreCase("PUT")) {

			// 0. First time set the value
			if (firstTimeExecution.getOrDefault(putKey, true)) {
				putBaseBucket.put(putKey, e);
				firstTimeExecution.put(putKey, false);
				
//				e.setPutBucketClose(e.getClose());
//				e.setPutBucketCallFlow(e.getCallFlow());
//				e.setPutBucketPutFlow(e.getPutFlow());
//				e.setPutBucketNetFlow(e.getNetFlow());
				
				return OptionFlow.builder()
						.option(e)
						.flow(FlowSignal.HOLD)
						.build();
			}

			firstTimeExecution.put(putKey, false);
			
			boolean putCloseCondition = (putBaseBucket.getOrDefault(putKey, e).getClose() <= e.getClose());

			if (putCloseCondition) {
				//OptionRsi putOption = putBaseBucket.getOrDefault(putKey, e);
				//putOption.setClose(e.getClose());
				//putBaseBucket.put(putKey, putOption);
				
//				e.setPutBucketClose(putOption.getClose());
//				e.setPutBucketCallFlow(putOption.getCallFlow());
//				e.setPutBucketPutFlow(putOption.getPutFlow());
//				e.setPutBucketNetFlow(putOption.getNetFlow());
				
				
				OptionRsi putOption = putBaseBucket.getOrDefault(putKey, e);
//				OptionRsi refOption = referenceBucket.getOrDefault(putKey, e);
//
//				// Individual bucket values
//				double putBucketCallFlow = putOption.getCallFlow();
//				double putBucketPutFlow  = putOption.getPutFlow();
//				double putBucketNetFlow  = putOption.getNetFlow();
//
//				double refBucketCallFlow = refOption.getCallFlow();
//				double refBucketPutFlow  = refOption.getPutFlow();
//				double refBucketNetFlow  = refOption.getNetFlow();
//
//				// Average values
//				double avgCallFlow = (putBucketCallFlow + refBucketCallFlow) / 2.0;
//				double avgPutFlow  = (putBucketPutFlow  + refBucketPutFlow) / 2.0;
//				double avgNetFlow  = (putBucketNetFlow  + refBucketNetFlow) / 2.0;
//				
//				refOption.setClose(e.getClose());
//				refOption.setCallFlow(avgCallFlow);
//				refOption.setPutFlow(avgPutFlow);
//				refOption.setNetFlow(avgNetFlow);
				
				putOption.setClose(e.getClose());
				putBaseBucket.put(putKey, putOption);
				
				return OptionFlow.builder()
						.option(e)
						.flow(FlowSignal.HOLD)
						.build();
			}

			Double putBucketNetFlow = putBaseBucket.getOrDefault(putKey, e).getNetFlow();
			Double putBucketPutFlow = putBaseBucket.getOrDefault(putKey, e).getPutFlow();

			Double currentPutNetFlow = e.getNetFlow();
			Double currentPutPutFlow = e.getPutFlow();

			if (currentPutNetFlow > putBucketNetFlow & currentPutPutFlow < putBucketPutFlow) {
				OptionRsi callOption = callBaseBucket.getOrDefault(callKey, e);
				OptionRsi putOption = putBaseBucket.getOrDefault(putKey, e);
				OptionRsi refOption  = referenceBucket.getOrDefault(callKey, e);

				// Individual bucket values
				double callBucketCallFlow = callOption.getCallFlow();
				double callBucketPutFlow  = callOption.getPutFlow();
				double callBucketNetFlow  = callOption.getNetFlow();

				double refBucketCallFlow = refOption.getCallFlow();
				double refBucketPutFlow  = refOption.getPutFlow();
				double refBucketNetFlow  = refOption.getNetFlow();

				// Average values
				double avgCallFlow = (callBucketCallFlow + refBucketCallFlow) / 2.0;
				double avgPutFlow  = (callBucketPutFlow  + refBucketPutFlow) / 2.0;
				double avgNetFlow  = (callBucketNetFlow  + refBucketNetFlow) / 2.0;
				
				//refOption.setClose(e.getClose());
				callOption.setCallFlow(avgCallFlow);
				callOption.setPutFlow(avgPutFlow);
				callOption.setNetFlow(avgNetFlow);
				
				
				putOption.setCallFlow(avgCallFlow);
				putOption.setPutFlow(avgPutFlow);
				putOption.setNetFlow(avgNetFlow);
				
				//update the call base bucket
				callBaseBucket.put(callKey, callOption);
				putBaseBucket.put(putKey, putOption);
				
//				e.setCallBucketClose(refOption.getClose());
//				e.setCallBucketCallFlow(refOption.getCallFlow());
//				e.setCallBucketPutFlow(refOption.getPutFlow());
//				e.setCallBucketNetFlow(refOption.getNetFlow());
				
				marketType = FlowSignal.SELL_PUT;
				
				return OptionFlow.builder()
						.option(e)
						.flow(FlowSignal.BUY_CALL)
						.build();
			}

		}

		return OptionFlow.builder()
				.option(e)
				.flow(FlowSignal.HOLD)
				.build();
	}
}