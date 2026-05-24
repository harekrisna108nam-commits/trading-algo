package com.example.dhan_rsi_series.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "money_flow", uniqueConstraints = @UniqueConstraint(columnNames = { "exchange", "security_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoneyFlow {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	// CALL / PUT / FUTURE (runtime value)
	private String optionType;
	private double callFlow;
	private double putFlow;
	private LocalDate expiryDate;
	private boolean active = true;
	private LocalDateTime createdAt = LocalDateTime.now();
}
