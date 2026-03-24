package com.example.dhan_rsi_series.utils;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.enums.FlowSignal;

/*@Service
public class FlowSignalService {

    private static final double NET_THRESHOLD   = 0;
    private static final double DELTA_THRESHOLD = 0;

    // =====================================================
    // GLOBAL FLOW + TARGET RSI MOMENTUM
    // =====================================================
    public FlowSignal evaluate(
            DpiAggregatorService.Snapshot snap,
            OptionRsi target
    ) {

        if (snap == null || target == null)
            return FlowSignal.HOLD;

        double net   = snap.net();               // call - put
        double delta = target.getDeltaRsi();

        // =============================
        // CALL SIDE
        // =============================
        if (net > NET_THRESHOLD && delta > DELTA_THRESHOLD)
            return FlowSignal.BUY_CALL;

        if (net < -NET_THRESHOLD || delta < -DELTA_THRESHOLD)
            return FlowSignal.SELL_CALL;

        // =============================
        // PUT SIDE
        // =============================
        if (net < -NET_THRESHOLD && delta < -DELTA_THRESHOLD)
            return FlowSignal.BUY_PUT;

        if (net > NET_THRESHOLD || delta > DELTA_THRESHOLD)
            return FlowSignal.SELL_PUT;

        return FlowSignal.HOLD;
    }
}*/

//@Service
//public class FlowSignalService {
//
//    private static final double NET_TH = 0;
//    private static final double DELTA_TH = 0;
//
//    public FlowSignal evaluate(
//            DpiAggregatorService.Snapshot snap,
//            OptionRsi ori
//    ) {
//
//        if (snap == null || ori == null )
//            return FlowSignal.HOLD;
//
//        double net = snap.net();
//
////        if (net > NET_TH) {
////            if (call.getDeltaRsi() > DELTA_TH) return FlowSignal.BUY_CALL;
////            if (put.getDeltaRsi() < DELTA_TH) return FlowSignal.SELL_PUT;
////        }
//        
//        if (ori.getOptionType().equalsIgnoreCase("CALL") && net>NET_TH && ori.getDeltaRsi() > DELTA_TH) {
//        	return FlowSignal.BUY_CALL;
//        }
//        
//        if (ori.getOptionType().equalsIgnoreCase("PUT") && net>NET_TH && ori.getDeltaRsi() < DELTA_TH) {
//        	return FlowSignal.SELL_PUT;
//        }
//        
//        if (ori.getOptionType().equalsIgnoreCase("PUT") && net<NET_TH && ori.getDeltaRsi() > DELTA_TH) {
//        	return FlowSignal.BUY_PUT;
//        }
//        
//        if (ori.getOptionType().equalsIgnoreCase("CALL") && net<NET_TH && ori.getDeltaRsi() < DELTA_TH) {
//        	return FlowSignal.SELL_CALL;
//        }
//
////        if (net < NET_TH) {
////            if (put.getDeltaRsi() > DELTA_TH) return FlowSignal.BUY_PUT;
////            if (call.getDeltaRsi() < DELTA_TH) return FlowSignal.SELL_CALL;
////        }
//
//        return FlowSignal.HOLD;
//    }
//}


//@Service
//public class FlowSignalService {
//
//    private static final double NET_TH = 0;
//    private static final double DELTA_TH = 0;
//
//    public FlowSignal evaluate(double net, double deltaRsi, String optionType) {
//
//        if (optionType.equalsIgnoreCase("CALL")) {
//
//            if (net > NET_TH && deltaRsi > DELTA_TH)
//                return FlowSignal.BUY_CALL;
//
//            if (net < NET_TH && deltaRsi < DELTA_TH)
//                return FlowSignal.SELL_CALL;
//
//        }
//
//        if (optionType.equalsIgnoreCase("PUT")) {
//
//            if (net > NET_TH && deltaRsi < DELTA_TH)
//                return FlowSignal.SELL_PUT;
//
//            if (net < NET_TH && deltaRsi > DELTA_TH)
//                return FlowSignal.BUY_PUT;
//
//        }
//
//        return FlowSignal.HOLD;
//    }
//}

@Service
public class FlowSignalService {

    // flow pressure threshold (2%)
    private static final double FLOW_TH = 0.0;

    // momentum threshold
    private static final double DELTA_RSI_TH = 0.0;

    public FlowSignal evaluate(
            double callFlow,
            double putFlow,
            double deltaRsi,
            String optionType
    ) {

        double net = callFlow - putFlow;
        double total = callFlow + putFlow;

        if (total == 0)
            return FlowSignal.HOLD;

        double normalizedFlow = net;

        // =====================================================
        // CALL logic
        // =====================================================
        if ("CALL".equalsIgnoreCase(optionType)) {

            // bullish pressure
            if (normalizedFlow > FLOW_TH && deltaRsi > DELTA_RSI_TH)
                return FlowSignal.BUY_CALL;

            // bearish pressure
            if (normalizedFlow > FLOW_TH && deltaRsi < -DELTA_RSI_TH)
                return FlowSignal.SELL_CALL;
        }

        // =====================================================
        // PUT logic
        // =====================================================
        if ("PUT".equalsIgnoreCase(optionType)) {

            // bullish market (sell puts)
            if (normalizedFlow < -FLOW_TH && deltaRsi < -DELTA_RSI_TH)
                return FlowSignal.SELL_PUT;

            // bearish market
            if (normalizedFlow < -FLOW_TH && deltaRsi > DELTA_RSI_TH)
                return FlowSignal.BUY_PUT;
        }

        return FlowSignal.HOLD;
    }
}