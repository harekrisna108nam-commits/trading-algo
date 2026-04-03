package com.example.dhan_rsi_series.mapper;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.model.Candle;
import com.example.dhan_rsi_series.model.CandleData;
import com.example.dhan_rsi_series.model.RsiCandle;
import com.example.dhan_rsi_series.utils.DateTimeUtil;

public class CandleMapper {

    public static List<CandleData> toCandleDataList(Candle candle) {

        if (candle == null || candle.getTimestamp() == null) {
            return Collections.emptyList();
        }

        int size = candle.getTimestamp().size();
        List<CandleData> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {

            CandleData data = new CandleData();

            data.setOpen(getValue(candle.getOpen(), i));
            data.setHigh(getValue(candle.getHigh(), i));
            data.setLow(getValue(candle.getLow(), i));
            data.setClose(getValue(candle.getClose(), i));
            data.setVolume(getValue(candle.getVolume(), i));

            Long ts = getValue(candle.getTimestamp(), i);
            data.setTimestamp(ts);
            data.setDateTime(DateTimeUtil.toIstDateTime(ts));

            data.setOpen_interest(getValue(candle.getOpen_interest(), i));

            result.add(data);
        }

        return result;
    }
    
    public static RsiCandle toCandleData(OptionRsi candle) {
        if (candle == null) {
            return null;
        }

        return RsiCandle.builder()
                // CandleData fields
                .open(candle.getOpen())
                .high(candle.getHigh())
                .low(candle.getLow())
                .close(candle.getClose())
                .volume(Double.valueOf(String.valueOf(candle.getSecurityId())))                 // not available in OptionRsi
                .open_interest(Double.valueOf(String.valueOf(candle.getOi())))          // not available in OptionRsi
                .timestamp(null)
                .dateTime(candle.getCandleTime())

                // RsiCandle fields
                .rsi(candle.getRsi())
                //.vwap(0.0) // or null / calculate elsewhere
                .deltaRsi(candle.getDeltaRsi())
                .callFlow(candle.getCallFlow())
                .putFlow(candle.getPutFlow())
                .netFlow(candle.getNetFlow())
                .highestOi(candle.getHighestOi())
                .atp(candle.getAtp())
                .deltaLtp(candle.getDeltaLtp())
                .deltaDeltaLtp(candle.getDeltaDeltaLtp())
                .buy(candle.isBuy())
                .sell(candle.isSell())
                //.deltaNet(candle.delta)
                .build();
    }


    private static <T> T getValue(List<T> list, int index) {
        return (list != null && index < list.size()) ? list.get(index) : null;
    }
}

