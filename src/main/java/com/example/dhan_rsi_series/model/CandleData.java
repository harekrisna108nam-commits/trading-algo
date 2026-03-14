package com.example.dhan_rsi_series.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CandleData {
	private Double open;
	private Double high;
	private Double low;
	private Double close;
	private Double volume;
	private Long timestamp;
	private LocalDateTime dateTime;
	private Double open_interest;
}

