package com.example.dhan_rsi_series.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
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
@Table(name = "option_chain")
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptionChain {
	@Id
    private String id;
	private String symbol;
	private int securityId;
	private String optionType;
	private String timeframe;     // 5S / 1M
    private LocalDateTime candleTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private int oi;
    private int highestOi;
    private double atp;
    private double dpi;     // put(delta(highestOi)*delta(atp)*close) - call(delta(highestOi)*delta(atp)*close)
    private double gamma;
    private LocalDate expiry;
}
