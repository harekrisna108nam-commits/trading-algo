package com.example.dhan_rsi_series.entity;

import java.time.LocalDateTime;

import com.example.dhan_rsi_series.utils.OptionTransactionListener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "option_transaction")
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(OptionTransactionListener.class)
public class OptionTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;        // NIFTY
    private int securityId;       // 58650
    private String optionType;    // CALL / PUT
    private String timeframe;     // 5S / 1M
    private boolean sold;
    private long position;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
 // =========================================
    // Copy constructor (VERY IMPORTANT)
    // =========================================
    public OptionTransaction(OptionTransaction other) {

        this.symbol = other.symbol;
        this.securityId = other.securityId;
        this.optionType = other.optionType;
        this.timeframe = other.timeframe;
        this.sold = other.sold;
        this.position = other.position; // MISSING
        this.active = other.active;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

}

