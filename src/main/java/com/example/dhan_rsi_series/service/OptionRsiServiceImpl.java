package com.example.dhan_rsi_series.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.mapper.CandleMapper;
import com.example.dhan_rsi_series.model.RsiCandle;
import com.example.dhan_rsi_series.repository.OptionRsiRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OptionRsiServiceImpl implements OptionRsiService {

    private final OptionRsiRepository optionRsiRepository;

    @Override
    public List<RsiCandle> getRsiCandles(String timeframe, String optionType) {

        return optionRsiRepository
                .findByTimeframeAndOptionType(timeframe, optionType)
                .stream()
                .map(CandleMapper::toCandleData) // static mapper
                .toList();
    }
}
