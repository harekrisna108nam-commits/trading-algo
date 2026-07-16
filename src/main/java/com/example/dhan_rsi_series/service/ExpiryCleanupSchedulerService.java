package com.example.dhan_rsi_series.service;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dhan_rsi_series.repository.DhanSubscriptionRepository;
import com.example.dhan_rsi_series.repository.MoneyFlowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiryCleanupSchedulerService {

    private final MoneyFlowRepository moneyFlowRepository;
    private final DhanSubscriptionRepository dhanSubscriptionRepository;

    @Transactional
    @Scheduled(cron = "0 14 7 * * *", zone = "Asia/Kolkata")
    public void cleanupExpiredData() {

        LocalDate today = LocalDate.now();

        int moneyFlowDeleted =
                moneyFlowRepository.deleteExpiredRecords(today);

        int subscriptionDeleted =
                dhanSubscriptionRepository.deleteExpiredRecords(today);

        log.info("✅ Deleted {} MoneyFlow expired records", moneyFlowDeleted);

        log.info("✅ Deleted {} DhanSubscription expired records",
                subscriptionDeleted);
    }
}