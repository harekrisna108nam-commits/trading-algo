package com.example.dhan_rsi_series.service;

import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.dhan_rsi_series.model.Instrument;
import com.example.dhan_rsi_series.utils.DhanLiveDataHandler;
import com.example.dhan_rsi_series.utils.DhanSubscriptionStore;
import com.example.dhan_rsi_series.utils.InstrumentParser;

import reactor.core.publisher.Flux;

@Service
public class InstrumentLoader {

	private final DhanMarketDataService dhanService;
	private final InstrumentParser parser;
	private final DhanSubscriptionStore store;
	private final DhanLiveDataHandler handler;

	public static Map<String, Instrument> instrumentMap;

	public InstrumentLoader(DhanMarketDataService dhanService, InstrumentParser parser, DhanSubscriptionStore store,
			DhanLiveDataHandler handler) {

		this.store = store;
		this.handler = handler;
		this.dhanService = dhanService;
		this.parser = parser;
	}

	// Monday to Friday at 9:00 AM
	//@Scheduled(cron = "0 0 9 ? * MON-FRI", zone = "Asia/Kolkata")
	@Scheduled(cron = "0 42 07 * * ?", zone = "Asia/Kolkata")
	public void loadInstruments() {

	    System.out.println("Downloading instrument file...");

	    String csv = dhanService.downloadInstrumentFile();

	    instrumentMap = parser.parse(csv);

	    System.out.println("Total Instruments Loaded: " + instrumentMap.size());


	    //.filter(entry->entry.getValue().getStrikePrice()>=20000 && entry.getValue().getStrikePrice()<=30000)
	    Flux.fromIterable(instrumentMap.entrySet())
	        // 1️⃣ persist
	        .doOnNext(entry ->
	            store.add(
	                entry.getValue().getExchangeSegment(),
	                entry.getKey(),
	                entry.getValue().getOptionType(),
	                entry.getValue().getStrikePrice()
	            )
	        )

	        // 2️⃣ subscribe socket
	        .flatMap(entry ->
	            handler.subscribe(
	                entry.getValue().getExchangeSegment(),
	                entry.getKey(),
	                entry.getValue().getOptionType(),
	                entry.getValue().getStrikePrice()
	            )
	        )

	        .doOnError(err -> System.err.println("Subscription error: " + err.getMessage()))

	        .subscribe();   // trigger execution
	}
}