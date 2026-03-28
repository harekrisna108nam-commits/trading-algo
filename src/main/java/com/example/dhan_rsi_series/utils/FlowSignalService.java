package com.example.dhan_rsi_series.utils;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.enums.FlowSignal;

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
            if (normalizedFlow < -FLOW_TH && deltaRsi < -DELTA_RSI_TH)
                return FlowSignal.SELL_CALL;
        }

        // =====================================================
        // PUT logic
        // =====================================================
        if ("PUT".equalsIgnoreCase(optionType)) {

            // bullish market (sell puts)
            if (normalizedFlow > FLOW_TH && deltaRsi < -DELTA_RSI_TH)
                return FlowSignal.SELL_PUT;

            // bearish market
            if (normalizedFlow < -FLOW_TH && deltaRsi > DELTA_RSI_TH)
                return FlowSignal.BUY_PUT;
        }

        return FlowSignal.HOLD;
    }
}