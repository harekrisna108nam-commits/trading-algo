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

        //====================================================================
        //---------------------------------CALL--------------------------------
        //====================================================================
		
		//0. First time set the value
        if (firstTimeExecution.get(callKey)) {
        	callBaseBucket.put(callKey, e);
            
            callBuyingBucket.put(callKey, e);
        }
        
        firstTimeExecution.put(callKey, false);
        
        //1. Call Base line Bucket replacement
        
        boolean closeCondition =  (callBaseBucket.get(callKey).getClose()>=e.getClose());
        boolean putFlowCondition =  (e.getPutFlow() >= callBaseBucket.get(callKey).getPutFlow());
        boolean netFlowCondition =  (e.getNetFlow() <= callBaseBucket.get(callKey).getNetFlow());
        
        if ( closeCondition & putFlowCondition & netFlowCondition ) {
        	callBaseBucket.put(callKey, e);
        }
        
        
        //2. Call Buying Bucket replacement
        boolean callBuyingCloseCondition =  (callBuyingBucket.get(callKey).getClose()>e.getClose());
        boolean callBuyingPutFlowCondition =  (e.getPutFlow() < callBuyingBucket.get(callKey).getPutFlow());
        boolean callBuyingNetFlowCondition =  (e.getNetFlow() >= callBaseBucket.get(callKey).getNetFlow());
        
        if ( callBuyingCloseCondition & callBuyingPutFlowCondition & callBuyingNetFlowCondition ) {
        	callBuyingBucket.put(callKey, e);
        }
        
        rocPutflow = (callBaseBucket.get(callKey).getPutFlow() - e.getPutFlow()) / Math.abs(callBaseBucket.get(callKey).getPutFlow());
        
        rocCallflow = (callBaseBucket.get(callKey).getCallFlow() - e.getCallFlow()) / Math.abs(callBaseBucket.get(callKey).getCallFlow());
        
        //3. Call Buying Execution
        
        boolean currCloseOpenCondition =  (e.getOpen() <= e.getClose());
        boolean rateFlowCondition = rocPutflow > rocCallflow;
        
        if (rateFlowCondition & currCloseOpenCondition & closeCondition & callBuyingPutFlowCondition & callBuyingNetFlowCondition ) {
        	callSellingBucket.put(callKey, callBuyingBucket.get(callKey));
        	return FlowSignal.BUY_CALL;
        }
        
        //4. Call Selling Bucket replacement
        boolean callSellingCloseCondition =  (callSellingBucket.get(callKey).getClose()<=e.getClose());
        boolean callSellingPutFlowCondition =  (e.getPutFlow() >= callSellingBucket.get(callKey).getPutFlow());
        boolean callSellingNetFlowCondition =  (e.getNetFlow() >= callBaseBucket.get(callKey).getNetFlow());
        
        if ( callSellingCloseCondition & callSellingPutFlowCondition & callSellingNetFlowCondition ) {
        	callSellingBucket.put(callKey, e);
        }
        
        
        //5. Call Selling Execution
        
        boolean currCloseOpenConditionForSelling =  (e.getOpen() >= e.getClose());

        
        if (currCloseOpenConditionForSelling & callSellingCloseCondition & callSellingPutFlowCondition & callSellingNetFlowCondition ) {
        	callBuyingBucket.put(callKey, callSellingBucket.get(callKey));
        	return FlowSignal.SELL_CALL;
        }
        
        //====================================================================
        //---------------------------------PUT--------------------------------
        //====================================================================
        
        //0. First time set the value
        if (firstTimeExecution.get(putKey)) {
        	putBaseBucket.put(putKey, e);
            
            putBuyingBucket.put(putKey, e);
        }
        
        firstTimeExecution.put(putKey, false);
        
        //1. Put Base line Bucket replacement
        
        boolean putCloseCondition =  (putBuyingBucket.get(putKey).getClose()>=e.getClose());
        boolean callFlowCondition =  (e.getCallFlow() >= putBaseBucket.get(putKey).getCallFlow());
        boolean netFlowConditionForPut =  (e.getNetFlow() >= putBaseBucket.get(putKey).getCallFlow());
        
        if ( putCloseCondition & callFlowCondition & netFlowConditionForPut ) {
        	putBaseBucket.put(putKey, e);
        }
        
        //2. Put Buying Bucket replacement
        //boolean putBuyingCloseCondition =  (putBuyingBucket.get(putKey).getClose()>=e.getClose());
        boolean putBuyingCallFlowCondition =  (e.getCallFlow() < putBuyingBucket.get(putKey).getCallFlow());
        boolean putBuyingNetFlowCondition =  (e.getNetFlow() <= putBaseBucket.get(putKey).getNetFlow());
        
        if ( putCloseCondition & putBuyingCallFlowCondition & putBuyingNetFlowCondition ) {
        	putBuyingBucket.put(putKey, e);
        }
        
        rocPutflow = (putBaseBucket.get(putKey).getPutFlow() - e.getPutFlow()) / Math.abs(putBaseBucket.get(putKey).getPutFlow());
        
        rocCallflow = (putBaseBucket.get(putKey).getCallFlow() - e.getCallFlow()) / Math.abs(putBaseBucket.get(putKey).getCallFlow());
        
        //3. Put Buying Execution

        boolean rateFlowConditionForPut = rocPutflow < rocCallflow;
        
        if (rateFlowConditionForPut & currCloseOpenCondition & putCloseCondition & putBuyingCallFlowCondition & putBuyingNetFlowCondition ) {
        	putSellingBucket.put(putKey, putBuyingBucket.get(putKey));
        	return FlowSignal.BUY_PUT;
        }
        
        //4. Put Selling Bucket replacement
        boolean putSellingCloseCondition =  (putBuyingBucket.get(putKey).getClose()<=e.getClose());
        boolean putSellingCallFlowCondition =  (e.getCallFlow() >= putBuyingBucket.get(putKey).getCallFlow());
        boolean putSellingNetFlowCondition =  (e.getNetFlow() <= putBuyingBucket.get(putKey).getNetFlow());
        
        if ( putSellingCloseCondition & putSellingCallFlowCondition & putSellingNetFlowCondition ) {
        	putSellingBucket.put(putKey, e);
        }
        
        
        //5. Put Selling Execution
        
        //boolean currCloseOpenConditionForSelling =  (e.getOpen() >= e.getClose());

        
        if (currCloseOpenConditionForSelling & putSellingCloseCondition & putSellingCallFlowCondition & putSellingNetFlowCondition ) {
        	putBuyingBucket.put(putKey, putSellingBucket.get(putKey));
        	return FlowSignal.SELL_PUT;
        }
		return FlowSignal.HOLD;
	}
}