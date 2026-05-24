package com.example.dhan_rsi_series.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.dhan_rsi_series.entity.DhanSubscription;

@Repository
public interface DhanSubscriptionRepository
        extends JpaRepository<DhanSubscription, Long> {

    // =========================================================
    // ACTIVE SUBSCRIPTIONS
    // =========================================================

    List<DhanSubscription> findByActiveTrue();

    // =========================================================
    // EXISTS
    // =========================================================

    boolean existsByExchangeAndSecurityId(
            String exchange,
            String securityId
    );

    // =========================================================
    // FULL ENTITY LOOKUP
    // =========================================================

    Optional<DhanSubscription> findByExchangeAndSecurityId(
            String exchange,
            String securityId
    );

    // =========================================================
    // SECURITY ID LOOKUP
    // =========================================================

    Optional<DhanSubscription> findBySecurityId(
            String securityId
    );

    // =========================================================
    // GAMMA
    // =========================================================

    @Query(value = """
            SELECT gamma
            FROM dhan_subscriptions
            WHERE security_id = :securityId
            """, nativeQuery = true)
    Double findGammaBySecurityId(
            @Param("securityId") String securityId
    );

    // =========================================================
    // STRIKE FROM SECURITY ID
    // =========================================================

    @Query(value = """
            SELECT strike
            FROM dhan_subscriptions
            WHERE security_id = :securityId
            """, nativeQuery = true)
    Integer findStrikeBySecurityId(
            @Param("securityId") String securityId
    );

    // =========================================================
    // EXPIRY FROM SECURITY ID
    // =========================================================

    @Query(value = """
            SELECT expiry_date
            FROM dhan_subscriptions
            WHERE security_id = :securityId
            """, nativeQuery = true)
    LocalDate findExpiryBySecurityId(
            @Param("securityId") String securityId
    );

    // =========================================================
    // OPTION TYPE FROM SECURITY ID
    // =========================================================

    @Query(value = """
            SELECT option_type
            FROM dhan_subscriptions
            WHERE security_id = :securityId
            """, nativeQuery = true)
    String findOptionTypeBySecurityId(
            @Param("securityId") String securityId
    );

    // =========================================================
    // SECURITY ID BY STRIKE + TYPE + EXPIRY
    // =========================================================

    @Query(value = """
            SELECT security_id
            FROM dhan_subscriptions
            WHERE strike = :strike
              AND option_type = :optionType
              AND expiry_date = :expiry
              AND active = true
            LIMIT 1
            """, nativeQuery = true)
    Integer findSecurityIdByStrikeAndExpiry(

            @Param("strike") int strike,

            @Param("optionType") String optionType,

            @Param("expiry") LocalDate expiry
    );

    // =========================================================
    // ALL DISTINCT EXPIRIES
    // =========================================================

    @Query(value = """
            SELECT DISTINCT expiry_date
            FROM dhan_subscriptions
            WHERE active = true
            ORDER BY expiry_date
            """, nativeQuery = true)
    List<LocalDate> findAllDistinctExpiries();

    // =========================================================
    // CURRENT EXPIRY
    // =========================================================

    @Query(value = """
            SELECT MIN(expiry_date)
            FROM dhan_subscriptions
            WHERE active = true
            """, nativeQuery = true)
    LocalDate findCurrentExpiry();

    // =========================================================
    // BY EXPIRY
    // =========================================================

    List<DhanSubscription> findByExpiryDateAndActiveTrue(
            LocalDate expiry
    );

    // =========================================================
    // BY OPTION TYPE + EXPIRY
    // =========================================================

    List<DhanSubscription> findByExpiryDateAndOptionTypeAndActiveTrue(
            LocalDate expiry,
            String optionType
    );
}