package com.example.dhan_rsi_series.model;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpiryStrike {
	private Integer strike;
	private LocalDate expiry;
}
