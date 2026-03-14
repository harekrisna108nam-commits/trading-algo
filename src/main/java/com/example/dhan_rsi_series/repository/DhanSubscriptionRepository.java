package com.example.dhan_rsi_series.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.dhan_rsi_series.entity.DhanSubscription;

@Repository
public interface DhanSubscriptionRepository
        extends JpaRepository<DhanSubscription, Long> {

    List<DhanSubscription> findByActiveTrue();

    boolean existsByExchangeAndSecurityId(
            String exchange, String securityId);
}

