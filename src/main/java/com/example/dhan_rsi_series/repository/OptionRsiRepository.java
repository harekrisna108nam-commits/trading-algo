package com.example.dhan_rsi_series.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dhan_rsi_series.entity.OptionRsi;

public interface OptionRsiRepository extends JpaRepository<OptionRsi, Long> {
	List<OptionRsi> findByTimeframeAndOptionType(String timeframe, String optionType);
}