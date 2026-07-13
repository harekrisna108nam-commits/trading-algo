package com.example.dhan_rsi_series.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "option_rsi")
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptionRsi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;        // NIFTY
    private int securityId;       // 58650
    private String optionType;    // CALL / PUT
    private String timeframe;     // 5S / 1M
    private LocalDateTime candleTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private double rsi;
    private int oi;
    private int highestOi;
    private double atp;
    private boolean buy;
    private boolean sell;
    private double dpi;     // put(delta(highestOi)*delta(atp)*close) - call(delta(highestOi)*delta(atp)*close)
    private double deltaRsi; // previousRsi - currentRsi
    private double callFlow;
    private double putFlow;
    private double netFlow;
    private double deltaLtp;
    private double deltaDeltaLtp;
    private double weightedOi;
    private double callWeightedOi;
    private double putWeightedOi;
    private double gamma;
    private int maxCallSecurityId;
    private int maxCallOi;
    private double maxCallClose;
    private int maxPutSecurityId;
    private int maxPutOi;
    private double maxPutClose;
    private LocalDate expiry;
    
 // =========================================
    // Copy constructor (VERY IMPORTANT)
    // =========================================
    public OptionRsi(OptionRsi other) {

        this.symbol = other.symbol;
        this.securityId = other.securityId;
        this.optionType = other.optionType;
        this.timeframe = other.timeframe;
        this.candleTime = other.candleTime;

        this.open = other.open;
        this.high = other.high;
        this.low = other.low;
        this.close = other.close;

        this.rsi = other.rsi;

        this.oi = other.oi;
        this.highestOi = other.highestOi;

        this.atp = other.atp;

        this.buy = other.buy;
        this.sell = other.sell;

        this.dpi = other.dpi;
        this.deltaRsi = other.deltaRsi;
        this.deltaLtp = other.deltaLtp;
        this.deltaDeltaLtp = other.deltaDeltaLtp;

        this.callFlow = other.callFlow;
        this.putFlow = other.putFlow;
        this.netFlow = other.netFlow;
        this.weightedOi = other.weightedOi;
        this.callWeightedOi = other.callWeightedOi;
        this.putWeightedOi = other.putWeightedOi;
        this.gamma = other.gamma;
        this.maxCallSecurityId = other.maxCallSecurityId;
        this.maxCallOi = other.maxCallOi;
        this.maxCallClose = other.maxCallClose;
        this.maxPutSecurityId = other.maxPutSecurityId;
        this.maxPutOi = other.maxPutOi;
        this.maxPutClose = other.maxPutClose;
    }

}

