package com.example.dhan_rsi_series.utils;

import java.time.LocalDateTime;

import com.example.dhan_rsi_series.entity.OptionTransaction;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class OptionTransactionListener {

    @PrePersist
    public void prePersist(OptionTransaction entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
    }

    @PreUpdate
    public void preUpdate(OptionTransaction entity) {
        entity.setUpdatedAt(LocalDateTime.now());
    }
}