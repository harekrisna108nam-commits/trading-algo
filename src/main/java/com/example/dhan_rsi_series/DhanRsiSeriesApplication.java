package com.example.dhan_rsi_series;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DhanRsiSeriesApplication {

	public static void main(String[] args) {
		SpringApplication.run(DhanRsiSeriesApplication.class, args);
	}

}
