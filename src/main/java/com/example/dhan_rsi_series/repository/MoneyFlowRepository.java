package com.example.dhan_rsi_series.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.dhan_rsi_series.entity.MoneyFlow;

@Repository
public interface MoneyFlowRepository
        extends JpaRepository<MoneyFlow, Long> {

    @Query("""
        SELECT m
        FROM MoneyFlow m
        WHERE m.active = true
    """)
    List<MoneyFlow> findAllActive();

    Optional<MoneyFlow> findByExpiryDate(LocalDate expiryDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM MoneyFlow m WHERE m.expiryDate < :today")
    int deleteExpiredRecords(LocalDate today);
}