package com.example.dhan_rsi_series.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dhan_rsi_series.entity.RsiEntity;

public interface RsiRepository extends JpaRepository<RsiEntity, Long> {
}
