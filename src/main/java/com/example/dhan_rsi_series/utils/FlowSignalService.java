package com.example.dhan_rsi_series.utils;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.entity.OptionTransaction;
import com.example.dhan_rsi_series.enums.FlowSignal;
import com.example.dhan_rsi_series.repository.OptionTransactionRepository;

import lombok.RequiredArgsConstructor;

//@Service
//public class FlowSignalService {
//
//    // flow pressure threshold (2%)
//    private static final double FLOW_TH = 0.0;
//
//    // momentum threshold
//    private static final double DELTA_RSI_TH = 0.0;
//
//    public FlowSignal evaluate(
//            double callFlow,
//            double putFlow,
//            double deltaRsi,
//            String optionType
//    ) {
//
//        double net = callFlow - putFlow;
//        double total = callFlow + putFlow;
//
//        if (total == 0)
//            return FlowSignal.HOLD;
//
//        double normalizedFlow = net;
//
//        // =====================================================
//        // CALL logic
//        // =====================================================
//        if ("CALL".equalsIgnoreCase(optionType)) {
//
//            // bullish pressure
//            if (normalizedFlow > FLOW_TH && deltaRsi > DELTA_RSI_TH)
//                return FlowSignal.BUY_CALL;
//
//            // bearish pressure
//            if (normalizedFlow < -FLOW_TH && deltaRsi < -DELTA_RSI_TH)
//                return FlowSignal.SELL_CALL;
//        }
//
//        // =====================================================
//        // PUT logic
//        // =====================================================
//        if ("PUT".equalsIgnoreCase(optionType)) {
//
//            // bullish market (sell puts)
//            if (normalizedFlow > FLOW_TH && deltaRsi < -DELTA_RSI_TH)
//                return FlowSignal.SELL_PUT;
//
//            // bearish market
//            if (normalizedFlow < -FLOW_TH && deltaRsi > DELTA_RSI_TH)
//                return FlowSignal.BUY_PUT;
//        }
//
//        return FlowSignal.HOLD;
//    }
//
//	public FlowSignal evaluate(double deltaDeltaLtp, double deltaLtp, double deltaNetFlow, Snapshot snap,
//			String optionType) {
//
//        double net = snap.netDpi();
////        double total = snap.callDpi() + snap.putDpi();
////
////        if (total == 0)
////            return FlowSignal.HOLD;
//
//        // =====================================================
//        // CALL logic
//        // =====================================================
//        if ("CALL".equalsIgnoreCase(optionType)) {
//
//            // bullish pressure
//            if (deltaDeltaLtp > 0 && deltaLtp > 0 && deltaNetFlow > 0 && net >= 0)
//                return FlowSignal.BUY_CALL;
//
//            // bearish pressure
//            if (deltaDeltaLtp < 0 && deltaLtp < 0 && deltaNetFlow < 0 && net <= 0)
//                return FlowSignal.SELL_CALL;
//        }
//
//        // =====================================================
//        // PUT logic
//        // =====================================================
//        if ("PUT".equalsIgnoreCase(optionType)) {
//
//            // bullish market (sell puts)
//        	if (deltaDeltaLtp < 0 && deltaLtp < 0 && deltaNetFlow > 0 && net >= 0)
//                return FlowSignal.SELL_PUT;
//
//            // bearish market
//        	if (deltaDeltaLtp > 0 && deltaLtp > 0 && deltaNetFlow < 0 && net <= 0)
//                return FlowSignal.BUY_PUT;
//        }
//
//        return FlowSignal.HOLD;
//	}
//}

@Service
@RequiredArgsConstructor
public class FlowSignalService {

	private final OptionTransactionRepository repository;

	public FlowSignal evaluate(DpiAggregatorService.Snapshot snap, String optionType, boolean callBuy, boolean putBuy) {

		double callFlow = snap.callDpi();
		double putFlow = snap.putDpi();
		double netFlow = snap.netDpi();
		double wtOi = snap.weightedOi();

		// =====================================================
		// CALL LOGIC
		// =====================================================
		if ("CALL".equalsIgnoreCase(optionType)) {
			
			OptionTransaction option = repository.findTopByOptionTypeOrderByCreatedAtDesc(optionType).orElse(null);
			
			boolean isEnableBuy = option != null && option.getPosition() == 0;

			boolean isEnableSell = option != null && option.getPosition() > 0;

			// BUY CALL
//            if (callBuy 
//            	|| (callFlow<0 && putFlow<0 && netFlow >=0)
//            	|| (putFlow>0 && callFlow>0 && netFlow >=0)) {
//
//                return FlowSignal.BUY_CALL;
//            }

			if ((callBuy || netFlow > 0) && isEnableBuy) {

				return FlowSignal.BUY_CALL;
			}

			// SELL CALL
//            if ((callFlow<0 && putFlow<0 && netFlow <=0)
//                || (putFlow>0 && callFlow>0 && netFlow <=0)) {
//
//                return FlowSignal.SELL_CALL;
//            }

			//if (netFlow < 0 && isEnableSell) { //later you have to work
			if (netFlow < 0 && isEnableSell) {

				return FlowSignal.SELL_CALL;
			}
		}

		// =====================================================
		// PUT LOGIC
		// =====================================================
		if ("PUT".equalsIgnoreCase(optionType)) {

			OptionTransaction option = repository.findTopByOptionTypeOrderByCreatedAtDesc(optionType).orElse(null);
			
			boolean isEnableBuy = option != null && option.getPosition() == 0;

			boolean isEnableSell = option != null && option.getPosition() > 0;

			// BUY PUT
//        	if (putBuy 
//                	|| (callFlow<0 && putFlow<0 && netFlow <=0)
//                	|| (putFlow>0 && callFlow>0 && netFlow <=0)) {
//
//                return FlowSignal.BUY_PUT;
//            }

			if ((putBuy || netFlow < 0) && isEnableBuy) {

				return FlowSignal.BUY_PUT;
			}

			// SELL PUT
//        	if ((callFlow<0 && putFlow<0 && netFlow >=0)
//                	|| (putFlow>0 && callFlow>0 && netFlow >=0)) {
//
//                return FlowSignal.SELL_PUT;
//            }

			//if (netFlow > 0 && isEnableSell) { // later you have to work
			if (netFlow > 0 && isEnableSell) {

				return FlowSignal.SELL_PUT;
			}
		}

		return FlowSignal.HOLD;
	}
}