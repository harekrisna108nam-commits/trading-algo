package com.example.dhan_rsi_series.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rsi_data")
public class RsiEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String instrumentId;
    private String optionType;
    private String timeframe;
    private long candleCloseTime;
    private double closePrice;
    private int rsiPeriod;
    private double rsiValue;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

