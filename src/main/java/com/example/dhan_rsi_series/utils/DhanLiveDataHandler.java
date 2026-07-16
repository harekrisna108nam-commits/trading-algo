package com.example.dhan_rsi_series.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.example.dhan_rsi_series.entity.DhanSubscription;
import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.entity.OptionTransaction;
import com.example.dhan_rsi_series.enums.FlowSignal;
import com.example.dhan_rsi_series.model.Tick;
import com.example.dhan_rsi_series.repository.OptionRsiRepository;
import com.example.dhan_rsi_series.repository.OptionTransactionRepository;
import com.example.dhan_rsi_series.service.CandleRsiService;
import com.example.dhan_rsi_series.service.DhanFundLimitService;
import com.example.dhan_rsi_series.service.DhanOrderService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

//@Slf4j
//@Component
//public class DhanLiveDataHandler implements WebSocketHandler {
//
//	private final CandleRsiService rsiService;
//	private final DhanSubscriptionStore store;
//	private final DpiAggregatorService aggregator;
//	private final FlowSignalService signalService;
//	private volatile WebSocketSession session;
//	private OptionRsiRepository rsiRepository;
//	private OptionTransactionRepository transactionRepository;
//	private final Map<Integer, OptionRsi> latestRsi = new ConcurrentHashMap<>();
//	private final Map<String, Double> lastNetMap = new ConcurrentHashMap<>();
//	private final Queue<OptionRsi> queue = new ConcurrentLinkedQueue<>();
//	private final List<OptionRsi> orderedLatest = new ArrayList<>();
//
//	private static final int FUTURE_ID = 66691; // replace actual
//
//	@Value("${dhan.client-id}")
//	private String clientId;
//
//	@Value("${dhan.access-token}")
//	private String accessToken;
//
//	private final DhanOrderService dhanOrderService;
//	private final DhanFundLimitService dhanFundLimitService;
//
//	public DhanLiveDataHandler(CandleRsiService rsiService, DhanSubscriptionStore store,
//			DpiAggregatorService aggregator, FlowSignalService signalService, OptionRsiRepository rsiRepository,
//			DhanOrderService dhanOrderService, OptionTransactionRepository transactionRepository,
//			DhanFundLimitService dhanFundLimitService) {
//		this.rsiService = rsiService;
//		this.store = store;
//		this.signalService = signalService;
//		this.aggregator = aggregator;
//		this.rsiRepository = rsiRepository;
//		this.dhanOrderService = dhanOrderService;
//		this.transactionRepository = transactionRepository;
//		this.dhanFundLimitService = dhanFundLimitService;
//	}
//
//	@Override
//	public Mono<Void> handle(WebSocketSession session) {
//
//		this.session = session;
//
//		// int callId = 62570;
//		// int putId = 62406;
//
//		// subscribe NIFTY SPOT
//		// subscribe("NSE_FNO", String.valueOf(FUTURE_ID), "FUTURE", 0); // FUT_IDX
//
//		Mono<Void> resubscribe = sendAllSubscriptions();
//
//		// =========================================================
//		// 1️⃣ Tick stream (safe decode)
//		// =========================================================
//		Flux<Tick> ticks = session.receive().filter(m -> m.getType() == WebSocketMessage.Type.BINARY)
//				.mapNotNull(m -> decode(m.getPayload())) // null safe
//				.filter(t -> t.oi() >= 1000 && t.ltp() >= 0.80) // OI range filter
//				.share();
//		
//		//.onBackpressureBuffer(5000, BufferOverflowStrategy.DROP_OLDEST)
//
//		// (t.securityId()!=74175 || t.securityId()!=74172) &&
//
//		// =========================================================
//		// 2️⃣ RSI stream (PARALLEL + NULL SAFE)
//		// =========================================================
//
//		Flux<OptionRsi> rsiFlux = ticks.onBackpressureLatest()
//
//				// 🔥 STRICT GLOBAL ORDER
//				.concatMap(t -> Mono.fromCallable(() -> {
//
//					// side TF
//					rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(), t.highestOi(), t.atp(),
//							4);
//
//					// main TF
//					return rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(), t.highestOi(),
//							t.atp(), 5);
//
//				}))
//
//				.filter(Objects::nonNull)
//
//				// 🔥 SINGLE THREAD = NO RACE
//				.publishOn(Schedulers.single())
//
//				.doOnNext(rsi -> {
//					latestRsi.put(rsi.getSecurityId(), rsi);
//
//					// 🔥 SAFE aggregation
//					aggregator.add(rsi, 5);
//				})
//
//				.doOnError(err -> log.error("❌ RSI ERROR", err))
//
//				.publish().refCount(1);
//
//		// =========================================================
//		// 3️ Aggregation (side-effect safe)
//		// =========================================================
//		// Mono<Void> aggregation = orderedRsiFlux.flatMap(r -> Mono.fromRunnable(() ->
//		// aggregator.add(r, 5))).then();
//
//		// =========================================================
//		// 4️ Decision every candle close
//		// =========================================================
/*Mono<Void> finalDecision = rsiFlux

		// 🔥 GROUP BY CANDLE CLOSE (CORE FIX)
		.bufferUntilChanged(OptionRsi::getCandleTime)

		// 🔥 STRICT SEQUENTIAL PROCESSING
		.concatMap(batch ->

		// ============================
		// 1️⃣ FETCH EXISTING TXN
		// ============================
		Mono.zip(
				Mono.fromCallable(
						() -> transactionRepository.findByTimeframeAndOptionTypeAndActive("5S", "CALL", true))
						.subscribeOn(Schedulers.boundedElastic()),

				Mono.fromCallable(
						() -> transactionRepository.findByTimeframeAndOptionTypeAndActive("5S", "PUT", true))
						.subscribeOn(Schedulers.boundedElastic()))

				.flatMap(txnTuple -> {

					Optional<OptionTransaction> callOpt = txnTuple.getT1();
					Optional<OptionTransaction> putOpt = txnTuple.getT2();

					// ============================
					// 2️⃣ RESOLVE SECURITY IDs
					// ============================
					Mono<Integer> callIdMono = callOpt.map(txn -> Mono.just(txn.getSecurityId()))

							.orElseGet(() -> Mono
									.fromSupplier(() -> batch.stream().filter(r -> r.getSecurityId() == 74175)
											.findFirst().orElse(latestRsi.get(74175)).getSecurityId()));
//									.orElseGet(() -> Mono.fromSupplier(() -> latestRsi.values().stream()
//											.filter(r -> "CALL".equalsIgnoreCase(r.getOptionType()))
//											.max(Comparator.comparing(OptionRsi::getOi)).map(OptionRsi::getSecurityId)
//											.orElse(74175) // fallback
//							));

					Mono<Integer> putIdMono = putOpt.map(txn -> Mono.just(txn.getSecurityId())).orElseGet(
							() -> Mono.fromSupplier(() -> batch.stream().filter(r -> r.getSecurityId() == 74172)
									.findFirst().orElse(latestRsi.get(74172)).getSecurityId()));
//									.orElseGet(() -> Mono.fromSupplier(() -> latestRsi.values().stream()
//											.filter(r -> "PUT".equalsIgnoreCase(r.getOptionType()))
//											.max(Comparator.comparing(OptionRsi::getOi)).map(OptionRsi::getSecurityId)
//											.orElse(74172) // fallback
//							));

					// ============================
					// 3️⃣ ENSURE TXN EXISTS
					// ============================
					Mono<OptionTransaction> callTxnMono = ensureTransaction(callOpt, "CALL", callIdMono);

					Mono<OptionTransaction> putTxnMono = ensureTransaction(putOpt, "PUT", putIdMono);

					// ============================
					// 4️⃣ COMBINE EVERYTHING
					// ============================
					return Mono.zip(callTxnMono, putTxnMono)

							.flatMap(txTuple -> {

								OptionTransaction callTxn = txTuple.getT1();
								OptionTransaction putTxn = txTuple.getT2();

								return Mono.fromCallable(() -> {

									if (batch.isEmpty())
										return null;

									// ============================
									// SNAPSHOT
									// ============================
									var snap = aggregator.snapshot(5);

									double callFlow = snap.callDpi();
									double putFlow = snap.putDpi();
									double netFlow = snap.netDpi();

									// ============================
									// FETCH RSI
									// ============================
									OptionRsi call = batch.stream()
											.filter(r -> r.getSecurityId() == callTxn.getSecurityId())
											.findFirst().orElse(latestRsi.get(callTxn.getSecurityId()));

									OptionRsi put = batch.stream()
											.filter(r -> r.getSecurityId() == putTxn.getSecurityId())
											.findFirst().orElse(latestRsi.get(putTxn.getSecurityId()));

									if (call == null || put == null) {
										log.warn("⚠ Missing CALL/PUT");
										return null;
									}

									OptionRsi callSave = new OptionRsi(call);
									OptionRsi putSave = new OptionRsi(put);

									// ============================
									// FLOW SET
									// ============================
									callSave.setCallFlow(callFlow);
									callSave.setPutFlow(putFlow);
									callSave.setNetFlow(netFlow);

									putSave.setCallFlow(callFlow);
									putSave.setPutFlow(putFlow);
									putSave.setNetFlow(netFlow);

									// ============================
									// BUY LOGIC
									// ============================
									Optional<OptionRsi> maxCall = latestRsi.values().stream()
											.filter(r -> "CALL".equalsIgnoreCase(r.getOptionType()))
											.max(Comparator.comparing(OptionRsi::getOi));

									Optional<OptionRsi> maxPut = latestRsi.values().stream()
											.filter(r -> "PUT".equalsIgnoreCase(r.getOptionType()))
											.max(Comparator.comparing(OptionRsi::getOi));

									boolean callBuy = maxPut.map(r -> r.getClose() <= 5).orElse(false);
									boolean putBuy = maxCall.map(r -> r.getClose() <= 5).orElse(false);

									// ============================
									// SIGNAL
									// ============================
									FlowSignal callSignal = signalService.evaluate(snap, "CALL", callBuy,
											putBuy);

									FlowSignal putSignal = signalService.evaluate(snap, "PUT", callBuy, putBuy);

									// ============================
									// EXECUTE (NOW SAFE)
									// ============================
									executeCallSignal(callSignal, callSave, Optional.of(callTxn));
									executePutSignal(putSignal, putSave, Optional.of(putTxn));

									return List.of(callSave, putSave);

								}).subscribeOn(Schedulers.boundedElastic());
							});
				})

				.flatMap(list -> {
					if (list == null)
						return Mono.empty();

					return Mono.fromCallable(() -> rsiRepository.saveAll(list))
							.subscribeOn(Schedulers.boundedElastic()).then();
				}))
		.then();*/
//
//		Mono<Void> finalDecision =
//
//				rsiFlux
//
//						// 🔥 GROUP BY CANDLE CLOSE (CORE FIX)
//						.bufferUntilChanged(OptionRsi::getCandleTime)
//
//						// 🔥 STRICT SEQUENTIAL PROCESSING
//						.concatMap(batch ->
//
//						Mono.fromCallable(() -> {
//
//							if (batch.isEmpty())
//								return null;
//
//							// ============================
//							// SNAPSHOT (CONSISTENT)
//							// ============================
//							var snap = aggregator.snapshot(5);
//
//							double callFlow = snap.callDpi();
//							double putFlow = snap.putDpi();
//							double netFlow = snap.netDpi();
//
//							log.info("📊 SNAP => call={} put={} net={}", callFlow, putFlow, netFlow);
//
//							// ============================
//							// GET CALL / PUT
//							// ============================
//							// 🔥 SAFE FETCH
//							OptionRsi call = batch.stream().filter(r -> r.getSecurityId() == 74175).findFirst()
//									.orElse(latestRsi.get(74175));
//
//							OptionRsi put = batch.stream().filter(r -> r.getSecurityId() == 74172).findFirst()
//									.orElse(latestRsi.get(74172));
//
//							if (call == null || put == null) {
//								log.warn("⚠ Missing CALL/PUT in batch");
//								return null;
//							}
//
//							OptionRsi callSave = new OptionRsi(call);
//							OptionRsi putSave = new OptionRsi(put);
//
//							callSave.setCallFlow(callFlow);
//							callSave.setPutFlow(putFlow);
//							callSave.setNetFlow(netFlow);
//							callSave.setCallWeightedOi(snap.callWeightedOi());
//
//							putSave.setCallFlow(callFlow);
//							putSave.setPutFlow(putFlow);
//							putSave.setNetFlow(netFlow);
//							putSave.setPutWeightedOi(snap.putWeightedOi());
//
//							// ============================
//							// MAX OI LOGIC
//							// ============================
//							Optional<OptionRsi> maxCall = latestRsi.values().stream()
//									.filter(r -> "CALL".equalsIgnoreCase(r.getOptionType()))
//									.max(Comparator.comparing(OptionRsi::getOi));
//
//							Optional<OptionRsi> maxPut = latestRsi.values().stream()
//									.filter(r -> "PUT".equalsIgnoreCase(r.getOptionType()))
//									.max(Comparator.comparing(OptionRsi::getOi));
//
//							maxCall.ifPresent(r -> {
//								callSave.setMaxCallSecurityId(r.getSecurityId());
//								callSave.setMaxCallOi(r.getOi());
//								callSave.setMaxCallClose(r.getClose());
//							});
//
//							maxPut.ifPresent(r -> {
//								putSave.setMaxPutSecurityId(r.getSecurityId());
//								putSave.setMaxPutOi(r.getOi());
//								putSave.setMaxPutClose(r.getClose());
//							});
//
//							boolean callBuy = maxPut.map(r -> r.getClose() <= 5).orElse(false);
//							boolean putBuy = maxCall.map(r -> r.getClose() <= 5).orElse(false);
//
//							FlowSignal callSignal = signalService.evaluate(snap, "CALL", callBuy, putBuy);
//
//							FlowSignal putSignal = signalService.evaluate(snap, "PUT", callBuy, putBuy);
//
//							executeCallSignal(callSignal, callSave, Optional.empty());
//							executePutSignal(putSignal, putSave, Optional.empty());
//
//							List<OptionRsi> toSave = Stream.of(callSave, putSave)
//									.sorted(Comparator.comparing(OptionRsi::getCandleTime)).toList();
//
//							// ============================
//							// 🔥 RESET AFTER SNAPSHOT
//							// ============================
//							// aggregator.reset(5);
//
//							return toSave;
//						})
//
//								.subscribeOn(Schedulers.boundedElastic())
//
//								.flatMap(list -> {
//									if (list == null)
//										return Mono.empty();
//
//									return Mono.fromCallable(() -> rsiRepository.saveAll(list))
//											.subscribeOn(Schedulers.boundedElastic())
//											.doOnSuccess(s -> log.info("✅ Saved {}", s.size())).then();
//								}))
//
//						.then();
//
//		// =====================================================
//		// 5️⃣ Heartbeat
//		// =====================================================
//		Mono<Void> heartbeat = session
//				.send(Flux.interval(Duration.ofSeconds(15)).map(i -> session.pingMessage(f -> f.allocateBuffer(0))));
//
//		// =====================================================
//		// 6️⃣ Lifecycle
//		// =====================================================
//
//		return resubscribe.then(Mono.when(rsiFlux.then(), // ingestion (runs forever)
//				finalDecision, // decision loop ✅ will trigger
//				heartbeat // keep alive
//		)).takeUntilOther(session.closeStatus()).doFinally(s -> this.session = null);
//
////		return resubscribe.then(Mono.when(rsiFlux.then(),   // ingestion
////				orderNewEngine,      // ordered processing
////		        finalDecision, 
////		        heartbeat).takeUntilOther(session.closeStatus()))
////				.doFinally(s -> this.session = null);
//	}
//
//	// =========================================
//	// Helper Method
//	// =========================================
//
//	private Mono<OptionTransaction> ensureTransaction(Optional<OptionTransaction> optTxn, String type,
//			Mono<Integer> idMono) {
//		if (optTxn.isPresent()) {
//			return Mono.just(optTxn.get());
//		} else {
//			return idMono.flatMap(id -> Mono
//					.fromCallable(
//							() -> transactionRepository.save(OptionTransaction.builder().active(true).optionType(type)
//									.timeframe("5S").securityId(id).position(0).createdAt(LocalDateTime.now()).build()))
//					.subscribeOn(Schedulers.boundedElastic()));
//		}
//	}
//
//	private void executeCallSignal(FlowSignal signal, OptionRsi callSave, Optional<OptionTransaction> callOptFinal) {
//
//		switch (signal) {
//
//		case BUY_CALL -> {
//			callSave.setBuy(true);
//
//			OptionTransaction optionTransaction = new OptionTransaction();
//			if (callOptFinal.isPresent()) {
//				optionTransaction = callOptFinal.get();
//				optionTransaction.setPosition(1);
//				optionTransaction.setUpdatedAt(LocalDateTime.now());
//			}
//			transactionRepository.save(optionTransaction);
//			
//			// placeCallOrder(buildRequest("BUY", callSave));
//		
//		
//		}
//
//		case SELL_CALL -> {
//			callSave.setSell(true);
//			// exitCall(buildRequest("SELL", callSave));
//			// if (!(callSave.getClose() >= 30 && callSave.getClose() <= 1200)) {
//
//			OptionTransaction optionTransaction = new OptionTransaction();
//			if (callOptFinal.isPresent()) {
//				optionTransaction = callOptFinal.get();
//				optionTransaction.setActive(false);
//			}
//			transactionRepository.save(optionTransaction);
//
//			// }
//		}
//
//		default -> {
//		}
//		}
//	}
//
//	private void executePutSignal(FlowSignal signal, OptionRsi putSave, Optional<OptionTransaction> putOptFinal) {
//
//		switch (signal) {
//
//		case BUY_PUT -> {
//			putSave.setBuy(true);
//			OptionTransaction optionTransaction = new OptionTransaction();
//			if (putOptFinal.isPresent()) {
//				optionTransaction = putOptFinal.get();
//				optionTransaction.setPosition(1);
//				optionTransaction.setUpdatedAt(LocalDateTime.now());
//			}
//			transactionRepository.save(optionTransaction);
//			// placePutOrder(buildRequest("BUY", putSave));
//		}
//
//		case SELL_PUT -> {
//			putSave.setSell(true);
//			// exitPut(buildRequest("SELL", putSave));
//			// if (!(putSave.getClose() >= 30 && putSave.getClose() <= 1200)) {
//
//			OptionTransaction optionTransaction = new OptionTransaction();
//			if (putOptFinal.isPresent()) {
//				optionTransaction = putOptFinal.get();
//				optionTransaction.setActive(false);
//			}
//			transactionRepository.save(optionTransaction);
//
//			// }
//		}
//
//		default -> {
//		}
//		}
//	}
//
//	private DhanOrderRequest buildRequest(String type, OptionRsi rsi) {
//
////		FundLimitResponse fund = dhanFundLimitService.getFundLimit()
////	            .subscribeOn(Schedulers.boundedElastic()) // safer thread
////	            .block();
////
////	    if (fund == null) {
////	        throw new RuntimeException("Failed to fetch fund limit");
////	    }
////
////	    double availableBalance = fund.getAvailabelBalance();
////
////	    if (availableBalance < 1000) {
////	        throw new RuntimeException("Insufficient balance");
////	    }
//
//		DhanOrderRequest request = DhanOrderRequest.builder().dhanClientId(clientId)
//				.correlationId(CorrelationIdGenerator.generate("NIFTY")).transactionType(type)
//				.exchangeSegment("NSE_FNO").productType("INTRADAY").orderType("MARKET").validity("DAY")
//				.securityId(String.valueOf(rsi.getSecurityId())).quantity(65).disclosedQuantity(0).price(0)
//				.triggerPrice(0).afterMarketOrder(false).build();
//
//		try {
//			ObjectMapper mapper = new ObjectMapper();
//			log.info("DHAN ORDER REQUEST => {}", mapper.writeValueAsString(request));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return request;
//	}
//
////	private Mono<DhanOrderRequest> buildRequest(String type, OptionRsi data) {
////
////	    return dhanFundLimitService.getFundLimit()
////	        .map(fund -> {
////
////	            double availableBalance = fund.getAvailabelBalance();
////
////	            // 👉 you can add validation here
////	            if (availableBalance < 1000) {
////	                throw new RuntimeException("Insufficient balance");
////	            }
////
////	            DhanOrderRequest request = DhanOrderRequest.builder()
////	                    .dhanClientId(clientId)
////	                    .correlationId(CorrelationIdGenerator.generate("NIFTY"))
////	                    .transactionType(type)
////	                    .exchangeSegment("NSE_FNO")
////	                    .productType("MARGIN")
////	                    .orderType("MARKET")
////	                    .validity("DAY")
////	                    .securityId(String.valueOf(data.getSecurityId()))
////	                    .quantity(65)
////	                    .disclosedQuantity(0)
////	                    .price(0)
////	                    .triggerPrice(0)
////	                    .afterMarketOrder(false)
////	                    .build();
////
////	            try {
////	                ObjectMapper mapper = new ObjectMapper();
////	                log.info("DHAN ORDER REQUEST => {}", mapper.writeValueAsString(request));
////	            } catch (Exception e) {
////	                log.error("Error serializing request", e);
////	            }
////
////	            return request;
////	        });
////	}
//
//	private void placeCallOrder(DhanOrderRequest request) {
//
//		System.out.println("🟢 BUY CALL");
//
//		dhanOrderService.placeOrder(accessToken, request)
//				.doOnSuccess(response -> System.out.println("BUY CALL :: " + response))
//				.doOnError(error -> System.out.println("❌ BUY CALL ERROR :: " + error.getMessage())).subscribe();
//	}
//
//	private void placePutOrder(DhanOrderRequest request) {
//
//		System.out.println("🔴 BUY PUT");
//
//		dhanOrderService.placeOrder(accessToken, request)
//				.doOnSuccess(response -> System.out.println("BUY PUT :: " + response))
//				.doOnError(error -> System.out.println("❌ BUY PUT ERROR :: " + error.getMessage())).subscribe();
//	}
//
//	private void exitCall(DhanOrderRequest request) {
//
//		System.out.println("⚪ EXIT CALL");
//
//		dhanOrderService.placeOrder(accessToken, request)
//				.doOnSuccess(response -> System.out.println("SELL CALL :: " + response))
//				.doOnError(error -> System.out.println("❌ SELL CALL ERROR :: " + error.getMessage())).subscribe();
//	}
//
//	private void exitPut(DhanOrderRequest request) {
//
//		System.out.println("⚪ EXIT PUT");
//
//		dhanOrderService.placeOrder(accessToken, request)
//				.doOnSuccess(response -> System.out.println("SELL PUT :: " + response))
//				.doOnError(error -> System.out.println("❌ SELL PUT ERROR :: " + error.getMessage())).subscribe();
//	}
//
//	// =====================================================
//	// ================= DECODE =================
//	// =====================================================
//	private Tick decode(DataBuffer buffer) {
//
//		byte[] bytes = new byte[buffer.readableByteCount()];
//		buffer.read(bytes);
//
//		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
//
//		if (bb.remaining() < 43)
//			return null;
//
//		bb.getShort();
//		bb.getShort();
//
//		int securityId = bb.getInt();
//		float ltp = bb.getFloat();
//
//		bb.getShort();
//		bb.getInt();
//
//		float atp = bb.getFloat();
//
//		bb.getInt();
//		bb.getInt();
//		bb.getInt();
//
//		int oi = bb.getInt();
//		int highestOi = bb.getInt();
//
//		String optionType;
//
//		if (securityId == FUTURE_ID) {
//			optionType = "FUTURE";
//		} else {
//			optionType = store.optionTypeOf(securityId);
//		}
//
//		// log.info("optionType::ltp {} {}", optionType, ltp);
//
//		if (optionType == null)
//			return null;
//
//		return new Tick(securityId, ltp, oi, highestOi, atp, optionType);
//	}
//
//	// ================= SUBSCRIBE =================
//
//	public Mono<Void> subscribe(String exchange, String securityId, String optionType, int strike) {
//
//		// 1️⃣ Persist + cache metadata
//		store.add(exchange, securityId, optionType, strike);
//
//		// 2️⃣ If WS not connected, DB restore will handle later
//		if (session == null || !session.isOpen()) {
//			return Mono.empty();
//		}
//
//		// 3️⃣ Live subscribe
//		return sendSubscription(exchange, securityId);
//	}
//
//	private Mono<Void> sendAllSubscriptions() {
//		// .filter(entry->entry.getStrike()>=20000 && entry.getStrike()<=30000)
//		return Flux.fromIterable(store.all()).flatMap(s -> sendSubscription(s.getExchange(), s.getSecurityId())).then();
//	}
//
//	private Mono<Void> sendSubscription(String exchange, String securityId) {
//
//		String payload = """
//				{
//				  "RequestCode": 21,
//				  "InstrumentCount": 1,
//				  "InstrumentList": [
//				    { "ExchangeSegment": "%s", "SecurityId": "%s" }
//				  ]
//				}
//				""".formatted(exchange, securityId);
//
//		return session.send(Mono.just(session.textMessage(payload)));
//	}
//}

/*@Slf4j
@Component
public class DhanLiveDataHandler implements WebSocketHandler {

	private final CandleRsiService rsiService;
	private final DhanSubscriptionStore store;
	private final DpiAggregatorService aggregator;
	private final FlowSignalService signalService;
	private final OptionRsiRepository rsiRepository;
	private final OptionTransactionRepository transactionRepository;
	private final DhanOrderService dhanOrderService;
	private final DhanFundLimitService dhanFundLimitService;

	private volatile WebSocketSession session;

	private final Map<Integer, OptionRsi> latestRsi = new ConcurrentHashMap<>();
	private final Map<String, Double> lastNetMap = new ConcurrentHashMap<>();
	private final Queue<OptionRsi> queue = new ConcurrentLinkedQueue<>();
	private final List<OptionRsi> orderedLatest = new ArrayList<>();

	private static final int FUTURE_ID = 66691;

	@Value("${dhan.client-id}")
	private String clientId;

	@Value("${dhan.access-token}")
	private String accessToken;

	public DhanLiveDataHandler(CandleRsiService rsiService, DhanSubscriptionStore store,
			DpiAggregatorService aggregator, FlowSignalService signalService, OptionRsiRepository rsiRepository,
			DhanOrderService dhanOrderService, OptionTransactionRepository transactionRepository,
			DhanFundLimitService dhanFundLimitService) {
		this.rsiService = rsiService;
		this.store = store;
		this.aggregator = aggregator;
		this.signalService = signalService;
		this.rsiRepository = rsiRepository;
		this.dhanOrderService = dhanOrderService;
		this.transactionRepository = transactionRepository;
		this.dhanFundLimitService = dhanFundLimitService;
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		this.session = session;

		// Subscribe FUTURE
		//subscribe("NSE_FNO", String.valueOf(FUTURE_ID), "FUTURE", 0);

		Mono<Void> resubscribe = sendAllSubscriptions();

		Flux<Tick> ticks = session.receive().filter(m -> m.getType() == WebSocketMessage.Type.BINARY)
				.mapNotNull(m -> decode(m.getPayload())).filter(t -> t.oi() >= 1000 && t.ltp() >= 0.80)
				.onBackpressureBuffer(5000, BufferOverflowStrategy.DROP_OLDEST).share();

		Flux<OptionRsi> rsiFlux = ticks.onBackpressureBuffer(5000, BufferOverflowStrategy.DROP_OLDEST)
				.concatMap(t -> Mono.fromCallable(() -> {
					rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(), t.highestOi(), t.atp(),
							4);

					return rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(), t.highestOi(),
							t.atp(), 5);
				})).filter(Objects::nonNull).publishOn(Schedulers.single()).doOnNext(rsi -> {
					latestRsi.put(rsi.getSecurityId(), rsi);
					aggregator.add(rsi, 5);
				}).doOnError(err -> log.error("❌ RSI ERROR", err)).publish().refCount(1);

		Mono<Void> finalDecision = rsiFlux

				// group by candle
				.bufferUntilChanged(OptionRsi::getCandleTime)

				// STRICT ORDER
				.concatMap(batch -> processBatch(batch))

				.then();

		Mono<Void> heartbeat = session
				.send(Flux.interval(Duration.ofSeconds(15)).map(i -> session.pingMessage(f -> f.allocateBuffer(0))));

		return resubscribe.then(Mono.when(rsiFlux.then(), finalDecision, heartbeat))
				.takeUntilOther(session.closeStatus()).doFinally(s -> this.session = null);
	}

	private Mono<Void> processBatch(List<OptionRsi> batch) {

		return Mono.fromCallable(() -> {

			if (batch.isEmpty())
				return null;

			// ================= SNAPSHOT =================
			var snap = aggregator.snapshot(5);

			double callFlow = snap.callDpi();
			double putFlow = snap.putDpi();
			double netFlow = snap.netDpi();

			// ================= FETCH TXN =================
			OptionTransaction callTxn = transactionRepository.findByTimeframeAndOptionTypeAndActive("5S", "CALL", true)
					.orElseGet(() -> transactionRepository.save(
			                newTxn("NIFTY", 41758, "CALL") // ✅ pass securityId
			        ));

			OptionTransaction putTxn = transactionRepository.findByTimeframeAndOptionTypeAndActive("5S", "PUT", true)
					.orElseGet(() -> transactionRepository.save(
			                newTxn("NIFTY", 41759, "PUT")
			        ));

			// ================= FETCH RSI =================
//			OptionRsi call = batch.stream().filter(r -> r.getSecurityId() == callTxn.getSecurityId()).findFirst()
//					.orElseThrow(() -> new RuntimeException("Missing CALL RSI"));
//
//			OptionRsi put = batch.stream().filter(r -> r.getSecurityId() == putTxn.getSecurityId()).findFirst()
//					.orElseThrow(() -> new RuntimeException("Missing PUT RSI"));
			
			OptionRsi call = batch.stream()
				    .filter(r -> r.getSecurityId() == callTxn.getSecurityId())
				    .findFirst()
				    .orElse(null);
				    //.orElseGet(() -> latestRsi.get(callTxn.getSecurityId()));

				OptionRsi put = batch.stream()
				    .filter(r -> r.getSecurityId() == putTxn.getSecurityId())
				    .findFirst()
				    .orElse(null);
				    //.orElseGet(() -> latestRsi.get(putTxn.getSecurityId()));
				
				if (call == null || put == null) {
				    log.warn("⚠ Skipping batch - missing RSI for txn. CALL={}, PUT={}",
				            callTxn.getSecurityId(), putTxn.getSecurityId());
				    return null; // skip safely
				}

			OptionRsi callSave = new OptionRsi(call);
			OptionRsi putSave = new OptionRsi(put);

			// ================= FLOW =================
			callSave.setCallFlow(callFlow);
			callSave.setPutFlow(putFlow);
			callSave.setNetFlow(netFlow);

			putSave.setCallFlow(callFlow);
			putSave.setPutFlow(putFlow);
			putSave.setNetFlow(netFlow);

			// ================= BUY LOGIC =================
			Optional<OptionRsi> maxCall = latestRsi.values().stream()
					.filter(r -> "CALL".equalsIgnoreCase(r.getOptionType()))
					.max(Comparator.comparing(OptionRsi::getOi));

			Optional<OptionRsi> maxPut = latestRsi.values().stream()
					.filter(r -> "PUT".equalsIgnoreCase(r.getOptionType())).max(Comparator.comparing(OptionRsi::getOi));

			boolean callBuy = maxPut.map(r -> r.getClose() <= 5).orElse(false);
			boolean putBuy = maxCall.map(r -> r.getClose() <= 5).orElse(false);

			// ================= SIGNAL =================
			FlowSignal callSignal = signalService.evaluate(snap, "CALL", callBuy, putBuy);
			FlowSignal putSignal = signalService.evaluate(snap, "PUT", callBuy, putBuy);

			// ================= APPLY SIGNAL TO RSI =================
			executeCallSignal(callSignal, callSave, Optional.of(callTxn));
			executePutSignal(putSignal, putSave, Optional.of(putTxn));

			// ================= UPDATE TRANSACTION =================
			updateCallTransaction(callTxn, callSignal);
			updatePutTransaction(putTxn, putSignal);

			// ================= SAVE =================
			rsiRepository.saveAll(List.of(callSave, putSave));

			return null;

		}).subscribeOn(Schedulers.boundedElastic()).then();
	}

	private OptionTransaction newTxn(String symbol, int securityId, String optionType) {

		return OptionTransaction.builder().symbol(symbol) // ✅ REQUIRED
				.securityId(securityId) // ✅ REQUIRED
				.optionType(optionType) // CALL / PUT
				.timeframe("5S") // fixed for now
				.position(0) // initial position
				.active(true) // active trade
				.sold(false) // not sold yet
				.createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
	}

	private void updateCallTransaction(OptionTransaction txn, FlowSignal signal) {

		switch (signal) {
		case BUY_CALL -> {
			txn.setPosition(1);
			txn.setUpdatedAt(LocalDateTime.now());
		}
		case SELL_CALL -> {
			txn.setActive(false);
		}
		default -> {
		}
		}

		transactionRepository.save(txn);
	}

	private void updatePutTransaction(OptionTransaction txn, FlowSignal signal) {

		switch (signal) {
		case BUY_PUT -> {
			txn.setPosition(1);
			txn.setUpdatedAt(LocalDateTime.now());
		}
		case SELL_PUT -> {
			txn.setActive(false);
		}
		default -> {
		}
		}

		transactionRepository.save(txn);
	}

	// ================= SIGNAL EXECUTION =================

	private void executeCallSignal(FlowSignal signal, OptionRsi callSave, Optional<OptionTransaction> callOptFinal) {
		switch (signal) {
		case BUY_CALL -> callSave.setBuy(true);
		case SELL_CALL -> callSave.setSell(true);
		default -> {
		}
		}
	}

	private void executePutSignal(FlowSignal signal, OptionRsi putSave, Optional<OptionTransaction> putOptFinal) {
		switch (signal) {
		case BUY_PUT -> putSave.setBuy(true);
		case SELL_PUT -> putSave.setSell(true);
		default -> {
		}
		}
	}

	// ================= ORDER =================

	private DhanOrderRequest buildRequest(String type, OptionRsi rsi) {

		DhanOrderRequest request = DhanOrderRequest.builder().dhanClientId(clientId)
				.correlationId(CorrelationIdGenerator.generate("NIFTY")).transactionType(type)
				.exchangeSegment("NSE_FNO").productType("INTRADAY").orderType("MARKET").validity("DAY")
				.securityId(String.valueOf(rsi.getSecurityId())).quantity(65).price(0).triggerPrice(0)
				.afterMarketOrder(false).build();

		try {
			ObjectMapper mapper = new ObjectMapper();
			log.info("DHAN ORDER REQUEST => {}", mapper.writeValueAsString(request));
		} catch (Exception e) {
			log.error("Serialization error", e);
		}

		return request;
	}

	// ================= DECODE =================

	private Tick decode(DataBuffer buffer) {
		byte[] bytes = new byte[buffer.readableByteCount()];
		buffer.read(bytes);

		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

		if (bb.remaining() < 43)
			return null;

		bb.getShort();
		bb.getShort();

		int securityId = bb.getInt();
		float ltp = bb.getFloat();

		bb.getShort();
		bb.getInt();

		float atp = bb.getFloat();

		bb.getInt();
		bb.getInt();
		bb.getInt();

		int oi = bb.getInt();
		int highestOi = bb.getInt();

		String optionType = (securityId == FUTURE_ID) ? "FUTURE" : store.optionTypeOf(securityId);

		if (optionType == null)
			return null;

		return new Tick(securityId, ltp, oi, highestOi, atp, optionType);
	}

	// ================= SUBSCRIBE =================

	public Mono<Void> subscribe(String exchange, String securityId, String optionType, Integer strike, LocalDate expiry) {

		store.add(exchange, securityId, optionType, strike, expiry);

		if (session == null || !session.isOpen()) {
			return Mono.empty();
		}

		return sendSubscription(exchange, securityId);
	}

	private Mono<Void> sendAllSubscriptions() {
		return Flux.fromIterable(store.all()).flatMap(s -> sendSubscription(s.getExchange(), s.getSecurityId())).then();
	}

	private Mono<Void> sendSubscription(String exchange, String securityId) {

		String payload = """
				{
				  "RequestCode": 21,
				  "InstrumentCount": 1,
				  "InstrumentList": [
				    {
				      "ExchangeSegment": "%s",
				      "SecurityId": "%s"
				    }
				  ]
				}
				""".formatted(exchange, securityId);

		return session.send(Mono.just(session.textMessage(payload)));
	}
}*/

//@Slf4j
//@Component
//public class DhanLiveDataHandler implements WebSocketHandler {
//
//    private final CandleRsiService rsiService;
//    private final DhanSubscriptionStore store;
//    private final DpiAggregatorService aggregator;
//    private final FlowSignalService signalService;
//    private final OptionRsiRepository rsiRepository;
//    private final OptionTransactionRepository transactionRepository;
//    private final DhanOrderService dhanOrderService;
//    private final DhanFundLimitService dhanFundLimitService;
//
//    private volatile WebSocketSession session;
//
//    // =========================================================
//    // GLOBAL CACHE
//    // =========================================================
//
//    private final Map<Integer, OptionRsi> latestRsi =
//            new ConcurrentHashMap<>();
//
//    //private static final int FUTURE_ID = 66691;
//
//    // =========================================================
//    // DEDICATED EXPIRY CORES
//    // =========================================================
//
//    private final Scheduler currentExpiryScheduler =
//            Schedulers.newSingle("CURRENT-EXPIRY");
//
//    private final Scheduler nextExpiryScheduler =
//            Schedulers.newSingle("NEXT-EXPIRY");
//    
//    private final Scheduler nextExpiryScheduler1 =
//            Schedulers.newSingle("NEXT-EXPIRY-1");
//    
//    private final Scheduler nextExpiryScheduler2 =
//            Schedulers.newSingle("NEXT-EXPIRY-2");
//    
//    private final Scheduler nextExpiryScheduler3 =
//            Schedulers.newSingle("NEXT-EXPIRY-3");
//    
//    private final Scheduler nextExpiryScheduler4 =
//            Schedulers.newSingle("NEXT-EXPIRY-4");
//
//    private final Scheduler farExpiryScheduler =
//            Schedulers.newSingle("FAR-EXPIRY");
//
//    // =========================================================
//
//    @Value("${dhan.client-id}")
//    private String clientId;
//
//    @Value("${dhan.access-token}")
//    private String accessToken;
//
//    public DhanLiveDataHandler(
//            CandleRsiService rsiService,
//            DhanSubscriptionStore store,
//            DpiAggregatorService aggregator,
//            FlowSignalService signalService,
//            OptionRsiRepository rsiRepository,
//            DhanOrderService dhanOrderService,
//            OptionTransactionRepository transactionRepository,
//            DhanFundLimitService dhanFundLimitService
//    ) {
//
//        this.rsiService = rsiService;
//        this.store = store;
//        this.aggregator = aggregator;
//        this.signalService = signalService;
//        this.rsiRepository = rsiRepository;
//        this.transactionRepository = transactionRepository;
//        this.dhanOrderService = dhanOrderService;
//        this.dhanFundLimitService = dhanFundLimitService;
//    }
//
//    // =========================================================
//    // HANDLE
//    // =========================================================
//
////    @Override
////    public Mono<Void> handle(WebSocketSession session) {
////
////        this.session = session;
////
////        Mono<Void> resubscribe = sendAllSubscriptions();
////
////        // =====================================================
////        // RAW TICKS
////        // =====================================================
////
////        Flux<Tick> ticks = session.receive()
////
////                .filter(m ->
////                        m.getType() ==
////                        WebSocketMessage.Type.BINARY
////                )
////
////                .mapNotNull(m ->
////                        decode(m.getPayload())
////                )
////
////                .filter(t ->
////                        t.oi() >= 1000 &&
////                        t.ltp() >= 0.80
////                )
////
////                .onBackpressureBuffer(
////                        50000,
////                        BufferOverflowStrategy.DROP_OLDEST
////                )
////
////                .share();
////
////        // =====================================================
////        // SPLIT BY EXPIRY
////        // =====================================================
////
////        Flux<GroupedFlux<LocalDate, Tick>> grouped =
////                ticks.groupBy(Tick::expiry);
////
////        // =====================================================
////        // PARALLEL EXPIRY PROCESSING
////        // =====================================================
////
////        Flux<OptionRsi> rsiFlux = grouped.flatMap(group -> {
////
////            LocalDate expiry = group.key();
////
////            Scheduler scheduler =
////                    resolveScheduler(expiry);
////
////            log.info(
////                    "✅ Expiry {} mapped to {}",
////                    expiry,
////                    scheduler
////            );
////
////            return group
////
////                    // -----------------------------------------
////                    // DEDICATED CORE
////                    // -----------------------------------------
////
////                    .publishOn(scheduler)
////
////                    // -----------------------------------------
////                    // STRICT ORDER INSIDE EXPIRY
////                    // -----------------------------------------
////
////                    .concatMap(t ->
////
////                            Mono.fromCallable(() -> {
////
////                                // ============================
////                                // 4 SECOND GAMMA
////                                // ============================
////
////                                rsiService.onLtp(
////                                        "NIFTY",
////                                        t.securityId(),
////                                        t.optionType(),
////                                        t.ltp(),
////                                        t.oi(),
////                                        t.highestOi(),
////                                        t.atp(),
////                                        4
////                                );
////
////                                // ============================
////                                // 5 SECOND RSI
////                                // ============================
////
////                                return rsiService.onLtp(
////                                        "NIFTY",
////                                        t.securityId(),
////                                        t.optionType(),
////                                        t.ltp(),
////                                        t.oi(),
////                                        t.highestOi(),
////                                        t.atp(),
////                                        5
////                                );
////                            })
////
////                    )
////
////                    .filter(Objects::nonNull)
////
////                    // -----------------------------------------
////                    // GLOBAL FLOW
////                    // ALL EXPIRIES
////                    // -----------------------------------------
////
////                    .doOnNext(rsi -> {
////
////                        latestRsi.put(
////                                rsi.getSecurityId(),
////                                rsi
////                        );
////
////                        aggregator.add(rsi, 5);
////                    })
////
////                    .doOnError(err ->
////                            log.error(
////                                    "❌ RSI ERROR",
////                                    err
////                            )
////                    );
////
////        }).publish().refCount(1);
////
////        // =====================================================
////        // FINAL DECISION
////        // CURRENT EXPIRY ONLY
////        // =====================================================
////
////        Flux<OptionRsi> currentExpiryFlux = rsiFlux
////
////                .filter(rsi ->
////                        store.isCurrentExpiry(
////                                rsi.getExpiry()
////                        )
////                );
////
////        Mono<Void> finalDecision = currentExpiryFlux
////
////                .bufferUntilChanged(
////                        OptionRsi::getCandleTime
////                )
////
////                // STRICT ORDER
////                .concatMap(this::processBatch)
////
////                .then();
////
////        // =====================================================
////        // HEARTBEAT
////        // =====================================================
////
////        Mono<Void> heartbeat = session.send(
////
////                Flux.interval(Duration.ofSeconds(15))
////
////                        .map(i ->
////                                session.pingMessage(
////                                        f -> f.allocateBuffer(0)
////                                )
////                        )
////        );
////
////        // =====================================================
////        // FINAL PIPELINE
////        // =====================================================
////
////        return resubscribe.then(
////
////                        Mono.when(
////                                rsiFlux.then(),
////                                finalDecision,
////                                heartbeat
////                        )
////                )
////
////                .takeUntilOther(
////                        session.closeStatus()
////                )
////
////                .doFinally(s ->
////                        this.session = null
////                );
////    }
//    
//    @Override
//    public Mono<Void> handle(WebSocketSession session) {
//
//        this.session = session;
//
//        log.info("🟢 WebSocket connected");
//
//        // =====================================================
//        // HEARTBEAT
//        // =====================================================
//
//        Flux<WebSocketMessage> heartbeatFlux =
//
//                Flux.interval(Duration.ofSeconds(15))
//
//                        .map(i -> {
//
//                            log.debug("💓 Sending Ping");
//
//                            return session.pingMessage(
//                                    factory -> factory.allocateBuffer(0)
//                            );
//                        });
//
//        // =====================================================
//        // RECEIVE TICKS
//        // =====================================================
//
//        Flux<Tick> ticks = session.receive()
//
//                .doOnSubscribe(s ->
//                        log.info("📡 Listening ticks...")
//                )
//
//                .doOnNext(msg ->
//                        log.debug("📩 WS Message Type: {}", msg.getType())
//                )
//
//                .doOnError(err ->
//                        log.error("❌ WebSocket receive error", err)
//                )
//
//                .doOnComplete(() ->
//                        log.warn("🔌 WebSocket disconnected")
//                )
//
//                .filter(msg ->
//                        msg.getType() ==
//                        WebSocketMessage.Type.BINARY
//                )
//
//                .mapNotNull(msg -> {
//
//                    try {
//                        return decode(msg.getPayload());
//
//                    } catch (Exception e) {
//
//                        log.error("❌ Decode error", e);
//
//                        return null;
//                    }
//                })
//
//                .filter(Objects::nonNull)
//
//                .filter(t ->
//                        t.oi() >= 1000 &&
//                        t.ltp() >= 0.80
//                )
//
//                .onBackpressureBuffer(
//                        50000,
//                        dropped ->
//                                log.warn("⚠ Tick dropped"),
//                        BufferOverflowStrategy.DROP_OLDEST
//                )
//
//                .share();
//
//        // =====================================================
//        // GROUP BY EXPIRY
//        // =====================================================
//
//        Flux<GroupedFlux<LocalDate, Tick>> grouped =
//                ticks.groupBy(Tick::expiry);
//
//        // =====================================================
//        // RSI PIPELINE
//        // =====================================================
//
//        Flux<OptionRsi> rsiFlux = grouped.flatMap(group -> {
//
//            LocalDate expiry = group.key();
//
//            Scheduler scheduler =
//                    resolveScheduler(expiry);
//
//            log.info(
//                    "✅ Expiry {} mapped to {}",
//                    expiry,
//                    scheduler
//            );
//
//            return group
//
//                    .publishOn(scheduler)
//
//                    .concatMap(t ->
//
//                            Mono.fromCallable(() -> {
//
//                                // 4 SEC
//                                rsiService.onLtp(
//                                        "NIFTY",
//                                        t.securityId(),
//                                        t.optionType(),
//                                        t.ltp(),
//                                        t.oi(),
//                                        t.highestOi(),
//                                        t.atp(),
//                                        4
//                                );
//
//                                // 5 SEC
//                                return rsiService.onLtp(
//                                        "NIFTY",
//                                        t.securityId(),
//                                        t.optionType(),
//                                        t.ltp(),
//                                        t.oi(),
//                                        t.highestOi(),
//                                        t.atp(),
//                                        5
//                                );
//
//                            }).subscribeOn(scheduler)
//                    )
//
//                    .filter(Objects::nonNull)
//
//                    .doOnNext(rsi -> {
//
//                        latestRsi.put(
//                                rsi.getSecurityId(),
//                                rsi
//                        );
//
//                        aggregator.add(rsi, 5);
//                    })
//
//                    .doOnError(err ->
//                            log.error(
//                                    "❌ RSI Processing Error",
//                                    err
//                            )
//                    );
//
//        });
//
//        // =====================================================
//        // FINAL DECISION
//        // =====================================================
//
//        Mono<Void> finalDecision = rsiFlux
//
//                .filter(rsi ->
//                        store.isCurrentExpiry(
//                                rsi.getExpiry()
//                        )
//                )
//
//                .doOnNext(rsi ->
//                        log.info(
//                                "🔥 FINAL RSI {} {}",
//                                rsi.getSecurityId(),
//                                rsi.getClose()
//                        )
//                )
//
//                .bufferUntilChanged(
//                        OptionRsi::getCandleTime
//                )
//
//                .concatMap(this::processBatch)
//
//                .then();
//
//        // =====================================================
//        // RESUBSCRIBE
//        // =====================================================
//
//        Mono<Void> subscribeMono = sendAllSubscriptions()
//
//                .doOnSuccess(v ->
//                        log.info("✅ All subscriptions restored")
//                );
//
//        // =====================================================
//        // SEND HEARTBEAT
//        // =====================================================
//
//        Mono<Void> heartbeatMono =
//                session.send(heartbeatFlux);
//
//        // =====================================================
//        // FINAL
//        // =====================================================
//
//        return subscribeMono.then(
//
//                        Mono.when(
//
//                                finalDecision,
//
//                                heartbeatMono
//                        )
//                )
//
//                .doFinally(signal -> {
//
//                    log.warn(
//                            "🔌 WebSocket closed {}",
//                            signal
//                    );
//
//                    this.session = null;
//                });
//    }
//
//    // =========================================================
//    // PROCESS BATCH
//    // ONLY CURRENT EXPIRY
//    // =========================================================
//
//    private Mono<Void> processBatch(List<OptionRsi> batch) {
//
//        return Mono.fromCallable(() -> {
//
//            if (batch.isEmpty()) {
//                return null;
//            }
//
//            // =================================================
//            // GLOBAL FLOW SNAPSHOT
//            // CURRENT + NEXT + FAR
//            // =================================================
//
//            var snap = aggregator.snapshot(5);
//
//            double callFlow = snap.callDpi();
//            double putFlow  = snap.putDpi();
//            double netFlow  = snap.netDpi();
//
//            // =================================================
//            // FETCH TXN
//            // =================================================
//
//            OptionTransaction callTxn =
//                    transactionRepository
//                            .findByTimeframeAndOptionTypeAndActive(
//                                    "5S",
//                                    "CALL",
//                                    true
//                            )
//                            .orElseGet(() ->
//                                    transactionRepository.save(
//                                            newTxn(
//                                                    "NIFTY",
//                                                    72171,
//                                                    "CALL"
//                                            )
//                                    )
//                            );
//
//            OptionTransaction putTxn =
//                    transactionRepository
//                            .findByTimeframeAndOptionTypeAndActive(
//                                    "5S",
//                                    "PUT",
//                                    true
//                            )
//                            .orElseGet(() ->
//                                    transactionRepository.save(
//                                            newTxn(
//                                                    "NIFTY",
//                                                    72172,
//                                                    "PUT"
//                                            )
//                                    )
//                            );
//
//            // =================================================
//            // CURRENT EXPIRY RSI ONLY
//            // =================================================
//
//            OptionRsi call = batch.stream()
//
//                    .filter(r ->
//                            r.getSecurityId() ==
//                            callTxn.getSecurityId()
//                    )
//
//                    .findFirst()
//
//                    .orElse(null);
//
//            OptionRsi put = batch.stream()
//
//                    .filter(r ->
//                            r.getSecurityId() ==
//                            putTxn.getSecurityId()
//                    )
//
//                    .findFirst()
//
//                    .orElse(null);
//
//            if (call == null || put == null) {
//
//                log.warn(
//                        "⚠ Missing RSI CALL={} PUT={}",
//                        callTxn.getSecurityId(),
//                        putTxn.getSecurityId()
//                );
//
//                return null;
//            }
//
//            OptionRsi callSave = new OptionRsi(call);
//            OptionRsi putSave  = new OptionRsi(put);
//
//            // =================================================
//            // APPLY GLOBAL FLOW
//            // =================================================
//
//            callSave.setCallFlow(callFlow);
//            callSave.setPutFlow(putFlow);
//            callSave.setNetFlow(netFlow);
//
//            putSave.setCallFlow(callFlow);
//            putSave.setPutFlow(putFlow);
//            putSave.setNetFlow(netFlow);
//
//            // =================================================
//            //                     BUY LOGIC
//            // =================================================
//
//            Optional<OptionRsi> maxCall =
//                    latestRsi.values().stream()
//
//                            .filter(r ->
//                                    "CALL".equalsIgnoreCase(
//                                            r.getOptionType()
//                                    )
//                            )
//
//                            .max(
//                                    Comparator.comparing(
//                                            OptionRsi::getOi
//                                    )
//                            );
//
//            Optional<OptionRsi> maxPut =
//                    latestRsi.values().stream()
//
//                            .filter(r ->
//                                    "PUT".equalsIgnoreCase(
//                                            r.getOptionType()
//                                    )
//                            )
//
//                            .max(
//                                    Comparator.comparing(
//                                            OptionRsi::getOi
//                                    )
//                            );
//
//            boolean callBuy =
//                    maxPut.map(r ->
//                            r.getClose() <= 5
//                    ).orElse(false);
//
//            boolean putBuy =
//                    maxCall.map(r ->
//                            r.getClose() <= 5
//                    ).orElse(false);
//
//            // =================================================
//            // SIGNAL
//            // =================================================
//
//            FlowSignal callSignal =
//                    signalService.evaluate(
//                            snap,
//                            "CALL",
//                            callBuy,
//                            putBuy
//                    );
//
//            FlowSignal putSignal =
//                    signalService.evaluate(
//                            snap,
//                            "PUT",
//                            callBuy,
//                            putBuy
//                    );
//
//            // =================================================
//            // APPLY SIGNAL
//            // =================================================
//
//            executeCallSignal(
//                    callSignal,
//                    callSave,
//                    Optional.of(callTxn)
//            );
//
//            executePutSignal(
//                    putSignal,
//                    putSave,
//                    Optional.of(putTxn)
//            );
//
//            // =================================================
//            // UPDATE TXN
//            // =================================================
//
//            updateCallTransaction(
//                    callTxn,
//                    callSignal
//            );
//
//            updatePutTransaction(
//                    putTxn,
//                    putSignal
//            );
//
//            // =================================================
//            // SAVE
//            // =================================================
//
//            rsiRepository.saveAll(
//                    List.of(
//                            callSave,
//                            putSave
//                    )
//            );
//
//            return null;
//
//        }).subscribeOn(
//                Schedulers.boundedElastic()
//        ).then();
//    }
//
//    // =========================================================
//    // SCHEDULER RESOLUTION
//    // =========================================================
//
//    private Scheduler resolveScheduler(LocalDate expiry) {
//
//        List<LocalDate> expiries =
//                store.allExpiriesSorted();
//
//        if (expiries.isEmpty()) {
//            return currentExpiryScheduler;
//        }
//
//        // CURRENT
//        if (expiry.equals(expiries.get(0))) {
//            return currentExpiryScheduler;
//        }
//
//        // NEXT
//        if (expiries.size() > 1 &&
//            expiry.equals(expiries.get(1))) {
//
//            return nextExpiryScheduler;
//        }
//
//        // NEXT-EXPIRY-1
//        if (expiries.size() > 2 &&
//            expiry.equals(expiries.get(2))) {
//
//            return nextExpiryScheduler1;
//        }
//        
//        // NEXT-EXPIRY-2
//        if (expiries.size() > 3 &&
//            expiry.equals(expiries.get(3))) {
//
//            return nextExpiryScheduler2;
//        }
//        
//        // NEXT-EXPIRY-3
//        if (expiries.size() > 4 &&
//            expiry.equals(expiries.get(4))) {
//
//            return nextExpiryScheduler3;
//        }
//        
//        // NEXT-EXPIRY-4
//        if (expiries.size() > 5 &&
//            expiry.equals(expiries.get(5))) {
//
//            return nextExpiryScheduler4;
//        }
//        
//        // FAR
//        return farExpiryScheduler;
//    }
//
//    // =========================================================
//    // TXN
//    // =========================================================
//
//    private OptionTransaction newTxn(
//            String symbol,
//            int securityId,
//            String optionType
//    ) {
//
//        return OptionTransaction.builder()
//
//                .symbol(symbol)
//
//                .securityId(securityId)
//
//                .optionType(optionType)
//
//                .timeframe("5S")
//
//                .position(0)
//
//                .active(true)
//
//                .sold(false)
//
//                .createdAt(LocalDateTime.now())
//
//                .updatedAt(LocalDateTime.now())
//
//                .build();
//    }
//
//    private void updateCallTransaction(
//            OptionTransaction txn,
//            FlowSignal signal
//    ) {
//
//        switch (signal) {
//
//            case BUY_CALL -> {
//
//                txn.setPosition(1);
//
//                txn.setUpdatedAt(
//                        LocalDateTime.now()
//                );
//            }
//
//            case SELL_CALL -> {
//
//                txn.setActive(false);
//            }
//
//            default -> {
//            }
//        }
//
//        transactionRepository.save(txn);
//    }
//
//    private void updatePutTransaction(
//            OptionTransaction txn,
//            FlowSignal signal
//    ) {
//
//        switch (signal) {
//
//            case BUY_PUT -> {
//
//                txn.setPosition(1);
//
//                txn.setUpdatedAt(
//                        LocalDateTime.now()
//                );
//            }
//
//            case SELL_PUT -> {
//
//                txn.setActive(false);
//            }
//
//            default -> {
//            }
//        }
//
//        transactionRepository.save(txn);
//    }
//
//    // =========================================================
//    // SIGNAL EXECUTION
//    // =========================================================
//
//    private void executeCallSignal(
//            FlowSignal signal,
//            OptionRsi callSave,
//            Optional<OptionTransaction> callOptFinal
//    ) {
//
//        switch (signal) {
//
//            case BUY_CALL ->
//                    callSave.setBuy(true);
//
//            case SELL_CALL ->
//                    callSave.setSell(true);
//
//            default -> {
//            }
//        }
//    }
//
//    private void executePutSignal(
//            FlowSignal signal,
//            OptionRsi putSave,
//            Optional<OptionTransaction> putOptFinal
//    ) {
//
//        switch (signal) {
//
//            case BUY_PUT ->
//                    putSave.setBuy(true);
//
//            case SELL_PUT ->
//                    putSave.setSell(true);
//
//            default -> {
//            }
//        }
//    }
//
//    // =========================================================
//    // ORDER
//    // =========================================================
//
//    private DhanOrderRequest buildRequest(
//            String type,
//            OptionRsi rsi
//    ) {
//
//        DhanOrderRequest request =
//                DhanOrderRequest.builder()
//
//                        .dhanClientId(clientId)
//
//                        .correlationId(
//                                CorrelationIdGenerator.generate(
//                                        "NIFTY"
//                                )
//                        )
//
//                        .transactionType(type)
//
//                        .exchangeSegment("NSE_FNO")
//
//                        .productType("INTRADAY")
//
//                        .orderType("MARKET")
//
//                        .validity("DAY")
//
//                        .securityId(
//                                String.valueOf(
//                                        rsi.getSecurityId()
//                                )
//                        )
//
//                        .quantity(65)
//
//                        .price(0)
//
//                        .triggerPrice(0)
//
//                        .afterMarketOrder(false)
//
//                        .build();
//
//        try {
//
//            ObjectMapper mapper =
//                    new ObjectMapper();
//
//            log.info(
//                    "DHAN ORDER REQUEST => {}",
//                    mapper.writeValueAsString(
//                            request
//                    )
//            );
//
//        } catch (Exception e) {
//
//            log.error(
//                    "Serialization error",
//                    e
//            );
//        }
//
//        return request;
//    }
//
//    // =========================================================
//    // DECODE
//    // =========================================================
//
//    private Tick decode(DataBuffer buffer) {
//
//        byte[] bytes =
//                new byte[
//                        buffer.readableByteCount()
//                ];
//
//        buffer.read(bytes);
//
//        ByteBuffer bb =
//                ByteBuffer.wrap(bytes)
//                        .order(ByteOrder.LITTLE_ENDIAN);
//
//        if (bb.remaining() < 43) {
//            return null;
//        }
//
//        bb.getShort();
//        bb.getShort();
//
//        int securityId = bb.getInt();
//
//        float ltp = bb.getFloat();
//
//        bb.getShort();
//        bb.getInt();
//
//        float atp = bb.getFloat();
//
//        bb.getInt();
//        bb.getInt();
//        bb.getInt();
//
//        int oi = bb.getInt();
//
//        int highestOi = bb.getInt();
//
//        DhanSubscription sub =
//                store.subscription(securityId);
//
//        if (sub == null) {
//            return null;
//        }
//
//        return new Tick(
//
//                securityId,
//
//                ltp,
//
//                oi,
//
//                highestOi,
//
//                atp,
//
//                sub.getOptionType(),
//
//                sub.getExpiryDate()
//        );
//    }
//
//    // =========================================================
//    // SUBSCRIBE
//    // =========================================================
//
//    public Mono<Void> subscribe(
//            String exchange,
//            String securityId,
//            String optionType,
//            Integer strike,
//            LocalDate expiry
//    ) {
//
//        store.add(
//                exchange,
//                securityId,
//                optionType,
//                strike,
//                expiry
//        );
//
//        if (session == null || !session.isOpen()) {
//            return Mono.empty();
//        }
//
//        return sendSubscription(
//                exchange,
//                securityId
//        );
//    }
//
//    private Mono<Void> sendAllSubscriptions() {
//
//        return Flux.fromIterable(store.all())
//
//                .flatMap(s ->
//                        sendSubscription(
//                                s.getExchange(),
//                                s.getSecurityId()
//                        )
//                )
//
//                .then();
//    }
//
//    private Mono<Void> sendSubscription(
//            String exchange,
//            String securityId
//    ) {
//
//        String payload = """
//                {
//                  "RequestCode": 21,
//                  "InstrumentCount": 1,
//                  "InstrumentList": [
//                    {
//                      "ExchangeSegment": "%s",
//                      "SecurityId": "%s"
//                    }
//                  ]
//                }
//                """.formatted(exchange, securityId);
//
//        return session.send(
//                Mono.just(
//                        session.textMessage(payload)
//                )
//        );
//    }
//}

@Slf4j
@Component
public class DhanLiveDataHandler implements WebSocketHandler {

	private final CandleRsiService rsiService;
	private final DhanSubscriptionStore store;
	private final DpiAggregatorService aggregator;
	private final UltraFlowSignalService signalService;
	private final OptionRsiRepository rsiRepository;
	private final OptionTransactionRepository transactionRepository;

	private volatile WebSocketSession session;

	// =========================================================
	// CACHE
	// =========================================================

	private final Map<Integer, OptionRsi> latestRsi = new ConcurrentHashMap<>();
	private final Map<Integer, OptionRsi> lastRsi = new ConcurrentHashMap<>();

	// =========================================================
	// EXPIRY THREADS
	// =========================================================

	private final Scheduler currentExpiryScheduler = Schedulers.newSingle("CURRENT-EXPIRY");

	private final Scheduler nextExpiryScheduler = Schedulers.newSingle("NEXT-EXPIRY");

	private final Scheduler nextExpiryScheduler1 = Schedulers.newSingle("NEXT-EXPIRY-1");

	private final Scheduler nextExpiryScheduler2 = Schedulers.newSingle("NEXT-EXPIRY-2");

	private final Scheduler nextExpiryScheduler3 = Schedulers.newSingle("NEXT-EXPIRY-3");

	private final Scheduler nextExpiryScheduler4 = Schedulers.newSingle("NEXT-EXPIRY-4");

	private final Scheduler nextExpiryScheduler5 = Schedulers.newSingle("NEXT-EXPIRY-5");

	private final Scheduler nextExpiryScheduler6 = Schedulers.newSingle("NEXT-EXPIRY-6");
	
	private final Scheduler farExpiryScheduler = Schedulers.newSingle("FAR-EXPIRY");

	// =========================================================

	public DhanLiveDataHandler(CandleRsiService rsiService, DhanSubscriptionStore store,
			DpiAggregatorService aggregator, UltraFlowSignalService signalService, OptionRsiRepository rsiRepository,
			OptionTransactionRepository transactionRepository) {

		this.rsiService = rsiService;
		this.store = store;
		this.aggregator = aggregator;
		this.signalService = signalService;
		this.rsiRepository = rsiRepository;
		this.transactionRepository = transactionRepository;
	}

	// =========================================================
	// HANDLE
	// =========================================================

	@Override
	public Mono<Void> handle(WebSocketSession session) {

		this.session = session;

		log.info("🟢 WebSocket connected");

		// =====================================================
		// RESUBSCRIBE
		// =====================================================

		Mono<Void> resubscribe = sendAllSubscriptions()

				.doOnSuccess(v -> log.info("✅ Subscriptions restored"));

		// =====================================================
		// HEARTBEAT
		// =====================================================

		Mono<Void> heartbeat = session.send(

				Flux.interval(Duration.ofSeconds(15))

						.map(i -> session.pingMessage(f -> f.allocateBuffer(0))));

		// =====================================================
		// RAW TICKS
		// =====================================================

		Flux<Tick> ticks = session.receive()

				.doOnSubscribe(s -> log.info("📡 Listening ticks..."))

				.doOnError(err -> log.error("❌ WebSocket error", err))

				.filter(msg -> msg.getType() == WebSocketMessage.Type.BINARY)

				.mapNotNull(msg -> decode(msg.getPayload()))

				.filter(t -> t.oi() >= 1000 && t.ltp() >= 0.80)

				.doOnNext(t -> log.info("📥 Tick {} {}", t.securityId(), t.expiry()))

				.onBackpressureBuffer(50000, dropped -> log.warn("⚠ Tick dropped"), BufferOverflowStrategy.DROP_OLDEST)

				.share();

		// =====================================================
		// GROUP BY EXPIRY
		// =====================================================

		Flux<GroupedFlux<LocalDate, Tick>> grouped = ticks.groupBy(Tick::expiry);

		// =====================================================
		// RSI ENGINE
		// =====================================================

		Flux<OptionRsi> rsiFlux = grouped.flatMap(group -> {

			LocalDate expiry = group.key();

			Scheduler scheduler = resolveScheduler(expiry);

			log.info("✅ Expiry {} mapped to {}", expiry, scheduler);

			return group

					// STRICT ORDER
					.concatMap(t ->

					Mono.fromCallable(() -> {

						log.info("🔥 Processing Tick {} {}", t.securityId(), expiry);

						// 4 SEC
						rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(), t.highestOi(),
								t.atp(), 4, expiry);

						// 5 SEC
						return rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(), t.highestOi(),
								t.atp(), 5, expiry);
					})

							// CRITICAL
							.subscribeOn(scheduler))

					.filter(Objects::nonNull)

					.doOnNext(rsi -> {

						log.info("✅ RSI {} {} {}", rsi.getSecurityId(), rsi.getOptionType(), rsi.getCandleTime());

						latestRsi.put(rsi.getSecurityId(), rsi);

						aggregator.add(rsi, 5);
					})

					.doOnError(err -> log.error("❌ RSI ERROR", err));

		}).share();

		// =====================================================
		// CURRENT EXPIRY ONLY
		// =====================================================

		Flux<OptionRsi> currentExpiryFlux = rsiFlux

				.filter(rsi -> {

					log.info("RSI EXPIRY={} CURRENT={}", rsi.getExpiry(), store.currentExpiry());

					boolean current = store.isCurrentExpiry(rsi.getExpiry());

					log.info("MATCH RESULT={}", current);

					if (current) {

						log.info("🎯 CURRENT EXPIRY {}", rsi.getSecurityId());
					}

					return current;
				});

		// =====================================================
		// FINAL DECISION
		// =====================================================

		Mono<Void> finalDecision = currentExpiryFlux

				// SINGLE ORDERED LANE
				.publishOn(Schedulers.single())

				// GROUP CANDLE
				.bufferUntilChanged(OptionRsi::getCandleTime)

				// STRICT ORDER
				.concatMap(this::processBatch)

				.doOnError(err -> log.error("❌ FINAL DECISION ERROR", err))

				.then();

		// =====================================================
		// PIPELINE
		// =====================================================

		return resubscribe.then(

				Mono.when(

						// IMPORTANT
						rsiFlux.then(),

						finalDecision,

						heartbeat))
				.takeUntilOther(session.closeStatus()).doFinally(signal -> {

					log.warn("🔌 WebSocket disconnected {}", signal);

					this.session = null;
				});
	}

	// =========================================================
	// PROCESS BATCH
	// =========================================================

	private Mono<Void> processBatch(List<OptionRsi> batch) {

		return Mono.fromCallable(() -> {

			if (batch.isEmpty()) {
				return null;
			}

			log.info("🔥 PROCESS BATCH SIZE={}", batch.size());

			// =================================================
			// SNAPSHOT
			// =================================================

			var snap = aggregator.snapshot(5);

			double prevCallDpi = snap.prevCallDpi();
			double prevPutDpi = snap.prevPutDpi();
			double currCallDpi = snap.currCallDpi();
			double currPutDpi = snap.currPutDpi();
			double netFlow = snap.netDpi();
			
			log.info("📊 FLOW previous CALL={} PUT={} NET={}", prevCallDpi, prevPutDpi, netFlow);

			log.info("📊 FLOW current CALL={} PUT={} NET={}", currCallDpi, currPutDpi, netFlow);

			// =================================================
			// TXN
			// =================================================

			OptionTransaction callTxn = transactionRepository.findByTimeframeAndOptionTypeAndActive("5S", "CALL", true)
					.orElseGet(() -> transactionRepository.save(newTxn("NIFTY", 57344, "CALL")));

			OptionTransaction putTxn = transactionRepository.findByTimeframeAndOptionTypeAndActive("5S", "PUT", true)
					.orElseGet(() -> transactionRepository.save(newTxn("NIFTY", 57345, "PUT")));

			// =================================================
			// FIND RSI
			// =================================================

			OptionRsi call = batch.stream()

					.filter(r -> r.getSecurityId() == callTxn.getSecurityId())

					.findFirst()

					.orElse(null);

			OptionRsi put = batch.stream()

					.filter(r -> r.getSecurityId() == putTxn.getSecurityId())

					.findFirst()

					.orElse(null);

			if (call == null || put == null) {

				log.warn("⚠ Missing RSI CALL={} PUT={}", callTxn.getSecurityId(), putTxn.getSecurityId());

				return null;
			}

			// =================================================
			// SAVE COPY
			// =================================================

			OptionRsi callSave = new OptionRsi(call);
			OptionRsi putSave = new OptionRsi(put);

			callSave.setCallFlow(currCallDpi);
			callSave.setPutFlow(currPutDpi);
			callSave.setNetFlow(netFlow);

			putSave.setCallFlow(currCallDpi);
			putSave.setPutFlow(currPutDpi);
			putSave.setNetFlow(netFlow);

			// ================= BUY LOGIC =================
//			Optional<OptionRsi> maxCall = batch.stream()
//					.filter(r -> "CALL".equalsIgnoreCase(r.getOptionType()))
//					.max(Comparator.comparing(OptionRsi::getOi));
//
//			Optional<OptionRsi> maxPut = batch.stream()
//					.filter(r -> "PUT".equalsIgnoreCase(r.getOptionType())).max(Comparator.comparing(OptionRsi::getOi));

			boolean callBuy = false;
			boolean putBuy = false;

			// ================= SIGNAL =================
			FlowSignal callSignal = signalService.evaluate(snap, callSave, lastRsi.getOrDefault(callSave.getSecurityId(), callSave), "CALL", callBuy, putBuy);
			FlowSignal putSignal = signalService.evaluate(snap, putSave, lastRsi.getOrDefault(putSave.getSecurityId(), putSave), "PUT", callBuy, putBuy);

			// ================= APPLY SIGNAL TO RSI =================
			executeCallSignal(putSignal, callSave, Optional.of(callTxn));
			executePutSignal(callSignal, putSave, Optional.of(putTxn));

			// ================= UPDATE TRANSACTION =================
			updateCallTransaction(callTxn, putSignal);
			updatePutTransaction(putTxn, callSignal);

			// ================= SAVE =================
			rsiRepository.saveAll(List.of(callSave, putSave));
			
			lastRsi.put(callSave.getSecurityId(), callSave);
			lastRsi.put(putSave.getSecurityId(), putSave);

			log.info("✅ SAVED");

			return null;

		}).subscribeOn(Schedulers.boundedElastic()).then();
	}

	private void updateCallTransaction(OptionTransaction txn, FlowSignal signal) {

		switch (signal) {

		case BUY_CALL -> {

			txn.setPosition(1);

			txn.setUpdatedAt(LocalDateTime.now());
		}

		case SELL_CALL -> {

			txn.setActive(false);
		}

		default -> {
		}
		}

		transactionRepository.save(txn);
	}

	private void updatePutTransaction(OptionTransaction txn, FlowSignal signal) {

		switch (signal) {

		case BUY_PUT -> {

			txn.setPosition(1);

			txn.setUpdatedAt(LocalDateTime.now());
		}

		case SELL_PUT -> {

			txn.setActive(false);
		}

		default -> {
		}
		}

		transactionRepository.save(txn);
	}

// =========================================================
// SIGNAL EXECUTION
// =========================================================

	private void executeCallSignal(FlowSignal signal, OptionRsi callSave, Optional<OptionTransaction> callOptFinal) {
		switch (signal) {
		case BUY_CALL -> callSave.setBuy(true);
		case SELL_CALL -> callSave.setSell(true);
		default -> {
		}
		}
	}

	private void executePutSignal(FlowSignal signal, OptionRsi putSave, Optional<OptionTransaction> putOptFinal) {
		switch (signal) {
		case BUY_PUT -> putSave.setBuy(true);
		case SELL_PUT -> putSave.setSell(true);
		default -> {
		}
		}
	}

	// =========================================================
	// RESOLVE SCHEDULER
	// =========================================================

	private Scheduler resolveScheduler(LocalDate expiry) {
		List<LocalDate> expiries = store.allExpiriesSorted();
		if (expiries.isEmpty()) {
			return currentExpiryScheduler;
		}

		if (expiry.equals(expiries.get(0))) {
			return currentExpiryScheduler;
		}

		if (expiries.size() > 1 && expiry.equals(expiries.get(1))) {
			return nextExpiryScheduler;
		}

		if (expiries.size() > 2 && expiry.equals(expiries.get(2))) {
			return nextExpiryScheduler1;
		}

		if (expiries.size() > 3 && expiry.equals(expiries.get(3))) {
			return nextExpiryScheduler2;
		}

		if (expiries.size() > 4 && expiry.equals(expiries.get(4))) {
			return nextExpiryScheduler3;
		}

		if (expiries.size() > 5 && expiry.equals(expiries.get(5))) {
			return nextExpiryScheduler4;
		}
		
		if (expiries.size() > 6 && expiry.equals(expiries.get(6))) {
			return nextExpiryScheduler5;
		}

		if (expiries.size() > 7 && expiry.equals(expiries.get(7))) {
			return nextExpiryScheduler6;
		}

		return farExpiryScheduler;
	}

	// =========================================================
	// NEW TXN
	// =========================================================

	private OptionTransaction newTxn(String symbol, int securityId, String optionType) {
		return OptionTransaction.builder()
				.symbol(symbol)
				.securityId(securityId)
				.optionType(optionType)
				.timeframe("5S")
				.position(0)
				.active(true)
				.sold(false)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	// =========================================================
	// DECODE
	// =========================================================

	private Tick decode(DataBuffer buffer) {

		byte[] bytes = new byte[buffer.readableByteCount()];

		buffer.read(bytes);

		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

		if (bb.remaining() < 43) {
			return null;
		}

		bb.getShort();
		bb.getShort();

		int securityId = bb.getInt();

		float ltp = bb.getFloat();

		bb.getShort();
		bb.getInt();

		float atp = bb.getFloat();

		bb.getInt();
		bb.getInt();
		bb.getInt();

		int oi = bb.getInt();

		int highestOi = bb.getInt();

		DhanSubscription sub = store.subscription(securityId);

		if (sub == null) {
			return null;
		}

		return new Tick(
				securityId,
				ltp,
				oi,
				highestOi,
				atp,
				sub.getOptionType(),
				sub.getExpiryDate());
	}

	// =========================================================
	// SUBSCRIBE
	// =========================================================

	public Mono<Void> subscribe(String exchange, String securityId, String optionType, Integer strike,
			LocalDate expiry) {

		store.add(exchange, securityId, optionType, strike, expiry);

		if (session == null || !session.isOpen()) {
			return Mono.empty();
		}

		return sendSubscription(exchange, securityId);
	}

	private Mono<Void> sendAllSubscriptions() {

		return Flux.fromIterable(store.all())
				.flatMap(s -> sendSubscription(s.getExchange(), s.getSecurityId()))
				.then();
	}

	private Mono<Void> sendSubscription(String exchange, String securityId) {

		String payload = """
				{
				  "RequestCode": 21,
				  "InstrumentCount": 1,
				  "InstrumentList": [
				    {
				      "ExchangeSegment": "%s",
				      "SecurityId": "%s"
				    }
				  ]
				}
				""".formatted(exchange, securityId);

		return session.send(Mono.just(session.textMessage(payload)));
	}
}