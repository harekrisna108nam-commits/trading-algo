package com.example.dhan_rsi_series.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dhan_rsi_series.entity.OptionTransaction;

public interface OptionTransactionRepository extends JpaRepository<OptionTransaction, Long> {
	Optional<OptionTransaction> findByTimeframeAndOptionTypeAndActive(String timeframe, String optionType, boolean active);
}