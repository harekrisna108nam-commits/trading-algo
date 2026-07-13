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
public class NewFlowSignalService {

	//private final OptionTransactionRepository repository;
	
    private final Map<String, Boolean> firstTimeExecution  = new ConcurrentHashMap<>();
    public final Map<String, OptionRsi> callBaseBucket = new ConcurrentHashMap<>();
    public final Map<String, OptionRsi> putBaseBucket = new ConcurrentHashMap<>();
    public final Map<String, OptionRsi> callBuyingBucket = new ConcurrentHashMap<>();
    public final Map<String, OptionRsi> putBuyingBucket = new ConcurrentHashMap<>();
    public final Map<String, OptionRsi> callSellingBucket = new ConcurrentHashMap<>();
    public final Map<String, OptionRsi> putSellingBucket = new ConcurrentHashMap<>();
    private final String callKey = "NIFTY" + "_" + "72171" + "_5";
	private final String putKey = "NIFTY" + "_" + "72172" + "_5";
	private Double rocPutflow = Double.MIN_NORMAL;
	private Double rocCallflow = Double.MIN_NORMAL;
    
    // =========================================================
    // 🔥 LOAD (ONE TIME)
    // =========================================================
    @PostConstruct
    public void load() {
        firstTimeExecution.put(callKey, true);
        firstTimeExecution.put(putKey, true);
    }
    
	public FlowSignal evaluate(DpiAggregatorService.Snapshot snap, OptionRsi e, OptionRsi optionRsiPrev, String optionType, boolean callBuy, boolean putBuy) {

        //=====================================================================
        //---------------------------------CALL--------------------------------
        //=====================================================================
		
		//0. First time set the value
        if (firstTimeExecution.getOrDefault(callKey, true)) {
        	callBaseBucket.put(callKey, e);
            
            callBuyingBucket.put(callKey, e);
        }
        
        firstTimeExecution.put(callKey, false);
        
        //1. Call Base line Bucket replacement
        
        boolean closeCondition =  (callBaseBucket.getOrDefault(callKey, e).getClose()>=e.getClose());
        boolean putFlowCondition =  (e.getPutFlow() >= callBaseBucket.getOrDefault(callKey, e).getPutFlow());
        boolean netFlowCondition =  (e.getNetFlow() <= callBaseBucket.getOrDefault(callKey, e).getNetFlow());
        
        if ( closeCondition & putFlowCondition & netFlowCondition ) {
        	callBaseBucket.put(callKey, e);
        }
        
        
        //2. Call Buying Bucket replacement
        boolean callBuyingCloseCondition =  (callBuyingBucket.getOrDefault(callKey, e).getClose()>e.getClose());
        boolean callBuyingPutFlowCondition =  (e.getPutFlow() < callBuyingBucket.getOrDefault(callKey, e).getPutFlow());
        boolean callBuyingNetFlowCondition =  (e.getNetFlow() >= callBaseBucket.getOrDefault(callKey, e).getNetFlow());
        
        if ( callBuyingCloseCondition & callBuyingPutFlowCondition & callBuyingNetFlowCondition ) {
        	callBuyingBucket.put(callKey, e);
        }
        
        rocPutflow = (callBaseBucket.getOrDefault(callKey, e).getPutFlow() - e.getPutFlow()) / Math.abs(callBaseBucket.getOrDefault(callKey, e).getPutFlow());
        
        rocCallflow = (callBaseBucket.getOrDefault(callKey, e).getCallFlow() - e.getCallFlow()) / Math.abs(callBaseBucket.getOrDefault(callKey, e).getCallFlow());
        
        //3. Call Buying Execution
        
        boolean currCloseOpenCondition =  (e.getOpen() <= e.getClose());
        boolean rateFlowCondition = rocPutflow > rocCallflow;
        
        if (rateFlowCondition & currCloseOpenCondition & closeCondition & callBuyingPutFlowCondition & callBuyingNetFlowCondition ) {
        	callSellingBucket.put(callKey, callBuyingBucket.getOrDefault(callKey, e));
        	return FlowSignal.BUY_CALL;
        }
        
        //4. Call Selling Bucket replacement
        boolean callSellingCloseCondition =  (callSellingBucket.getOrDefault(callKey, e).getClose()<=e.getClose());
        boolean callSellingPutFlowCondition =  (e.getPutFlow() >= callSellingBucket.getOrDefault(callKey, e).getPutFlow());
        boolean callSellingNetFlowCondition =  (e.getNetFlow() >= callBaseBucket.getOrDefault(callKey, e).getNetFlow());
        
        if ( callSellingCloseCondition & callSellingPutFlowCondition & callSellingNetFlowCondition ) {
        	callSellingBucket.put(callKey, e);
        }
        
        
        //5. Call Selling Execution
        
        boolean currCloseOpenConditionForSelling =  (e.getOpen() >= e.getClose());

        
        if (currCloseOpenConditionForSelling & callSellingCloseCondition & callSellingPutFlowCondition & callSellingNetFlowCondition ) {
        	callBuyingBucket.put(callKey, callSellingBucket.getOrDefault(callKey, e));
        	return FlowSignal.SELL_CALL;
        }
        
        //====================================================================
        //---------------------------------PUT--------------------------------
        //====================================================================
        
        //0. First time set the value
        if (firstTimeExecution.getOrDefault(putKey, true)) {
        	putBaseBucket.put(putKey, e);
            
            putBuyingBucket.put(putKey, e);
        }
        
        firstTimeExecution.put(putKey, false);
        
        //1. Put Base line Bucket replacement
        
        boolean putCloseCondition =  (putBuyingBucket.getOrDefault(putKey, e).getClose()>=e.getClose());
        boolean callFlowCondition =  (e.getCallFlow() >= putBaseBucket.getOrDefault(putKey, e).getCallFlow());
        boolean netFlowConditionForPut =  (e.getNetFlow() >= putBaseBucket.getOrDefault(putKey, e).getCallFlow());
        
        if ( putCloseCondition & callFlowCondition & netFlowConditionForPut ) {
        	putBaseBucket.put(putKey, e);
        }
        
        //2. Put Buying Bucket replacement
        //boolean putBuyingCloseCondition =  (putBuyingBucket.get(putKey).getClose()>=e.getClose());
        boolean putBuyingCallFlowCondition =  (e.getCallFlow() < putBuyingBucket.getOrDefault(putKey, e).getCallFlow());
        boolean putBuyingNetFlowCondition =  (e.getNetFlow() <= putBaseBucket.getOrDefault(putKey, e).getNetFlow());
        
        if ( putCloseCondition & putBuyingCallFlowCondition & putBuyingNetFlowCondition ) {
        	putBuyingBucket.put(putKey, e);
        }
        
        rocPutflow = (putBaseBucket.getOrDefault(putKey, e).getPutFlow() - e.getPutFlow()) / Math.abs(putBaseBucket.getOrDefault(putKey, e).getPutFlow());
        
        rocCallflow = (putBaseBucket.getOrDefault(putKey, e).getCallFlow() - e.getCallFlow()) / Math.abs(putBaseBucket.getOrDefault(putKey, e).getCallFlow());
        
        //3. Put Buying Execution

        boolean rateFlowConditionForPut = rocPutflow < rocCallflow;
        
        if (rateFlowConditionForPut & currCloseOpenCondition & putCloseCondition & putBuyingCallFlowCondition & putBuyingNetFlowCondition ) {
        	putSellingBucket.put(putKey, putBuyingBucket.getOrDefault(putKey, e));
        	return FlowSignal.BUY_PUT;
        }
        
        //4. Put Selling Bucket replacement
        boolean putSellingCloseCondition =  (putBuyingBucket.getOrDefault(putKey, e).getClose()<=e.getClose());
        boolean putSellingCallFlowCondition =  (e.getCallFlow() >= putBuyingBucket.getOrDefault(putKey, e).getCallFlow());
        boolean putSellingNetFlowCondition =  (e.getNetFlow() <= putBuyingBucket.getOrDefault(putKey, e).getNetFlow());
        
        if ( putSellingCloseCondition & putSellingCallFlowCondition & putSellingNetFlowCondition ) {
        	putSellingBucket.put(putKey, e);
        }
        
        
        //5. Put Selling Execution
        
        //boolean currCloseOpenConditionForSelling =  (e.getOpen() >= e.getClose());

        
        if (currCloseOpenConditionForSelling & putSellingCloseCondition & putSellingCallFlowCondition & putSellingNetFlowCondition ) {
        	putBuyingBucket.put(putKey, putSellingBucket.getOrDefault(putKey, e));
        	return FlowSignal.SELL_PUT;
        }
		return FlowSignal.HOLD;
	}
}