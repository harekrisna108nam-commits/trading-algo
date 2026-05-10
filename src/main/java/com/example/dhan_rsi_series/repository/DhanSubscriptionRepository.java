package com.example.dhan_rsi_series.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.dhan_rsi_series.entity.DhanSubscription;

@Repository
public interface DhanSubscriptionRepository
        extends JpaRepository<DhanSubscription, Long> {

    List<DhanSubscription> findByActiveTrue();

    boolean existsByExchangeAndSecurityId(
            String exchange, String securityId);

    
    @Query(value = "SELECT gamma FROM dhan_subscriptions WHERE security_id = :securityId", nativeQuery = true)
    Double findGammaBySecurityId(@Param("securityId") String securityId);

 // =========================================
    // 🔥 GET STRIKE FROM SECURITY ID
    // =========================================
    @Query(value = """
        SELECT strike 
        FROM dhan_subscriptions 
        WHERE security_id = :securityId
        """, nativeQuery = true)
    Integer findStrikeBySecurityId(@Param("securityId") String securityId);

    // =========================================
    // 🔥 GET SECURITY ID FROM STRIKE + TYPE
    // =========================================
    @Query(value = """
        SELECT security_id 
        FROM dhan_subscriptions 
        WHERE strike = :strike 
        AND option_type = :optionType
        AND active = true
        LIMIT 1
        """, nativeQuery = true)
    Integer findSecurityIdByStrike(
            @Param("strike") int strike,
            @Param("optionType") String optionType
    );
}

