package com.example.dhan_rsi_series.model;

import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.enums.FlowSignal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OptionFlow {
	private OptionRsi option;
	private FlowSignal flow;
}
