package com.example.dhan_rsi_series.entity;

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
@Table(name = "dhan_subscriptions", uniqueConstraints = @UniqueConstraint(columnNames = { "exchange", "security_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DhanSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String exchange;

	@Column(name = "security_id", nullable = false)
	private String securityId;

	// CALL / PUT (runtime value)
	private String optionType;
	
	private int strike;
	
	private double gamma;

	private boolean active = true;

	private LocalDateTime createdAt = LocalDateTime.now();
}