package com.example.dhan_rsi_series.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.entity.OptionTransaction;
import com.example.dhan_rsi_series.enums.FlowSignal;
import com.example.dhan_rsi_series.model.DhanOrderRequest;
import com.example.dhan_rsi_series.model.Tick;
import com.example.dhan_rsi_series.repository.OptionRsiRepository;
import com.example.dhan_rsi_series.repository.OptionTransactionRepository;
import com.example.dhan_rsi_series.service.CandleRsiService;
import com.example.dhan_rsi_series.service.DhanFundLimitService;
import com.example.dhan_rsi_series.service.DhanOrderService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class DhanLiveDataHandler implements WebSocketHandler {

	private final CandleRsiService rsiService;
	private final DhanSubscriptionStore store;
	private final DpiAggregatorService aggregator;
	private final FlowSignalService signalService;
	private volatile WebSocketSession session;
	private OptionRsiRepository rsiRepository;
	private OptionTransactionRepository transactionRepository;
	private final Map<Integer, OptionRsi> latestRsi = new ConcurrentHashMap<>();

	@Value("${dhan.client-id}")
	private String clientId;

	@Value("${dhan.access-token}")
	private String accessToken;

	private final DhanOrderService dhanOrderService;
	private final DhanFundLimitService dhanFundLimitService;

	public DhanLiveDataHandler(CandleRsiService rsiService, DhanSubscriptionStore store,
			DpiAggregatorService aggregator, FlowSignalService signalService, OptionRsiRepository rsiRepository,
			DhanOrderService dhanOrderService, OptionTransactionRepository transactionRepository,
			DhanFundLimitService dhanFundLimitService) {
		this.rsiService = rsiService;
		this.store = store;
		this.signalService = signalService;
		this.aggregator = aggregator;
		this.rsiRepository = rsiRepository;
		this.dhanOrderService = dhanOrderService;
		this.transactionRepository = transactionRepository;
		this.dhanFundLimitService = dhanFundLimitService;
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {

		this.session = session;

		Mono<Void> resubscribe = sendAllSubscriptions();

		// =========================================================
		// 1️⃣ Tick stream (safe decode)
		// =========================================================
		Flux<Tick> ticks = session.receive().filter(m -> m.getType() == WebSocketMessage.Type.BINARY)
				.mapNotNull(m -> decode(m.getPayload())) // null safe
				.filter(t -> t.atp() >= 10 && t.atp() <= 300) // LTP range filter
				.share();

		// =========================================================
		// 2️⃣ RSI stream (PARALLEL + NULL SAFE)
		// =========================================================
		Flux<OptionRsi> rsiFlux = ticks.groupBy(Tick::securityId)
				.flatMap(group -> group.publishOn(Schedulers.parallel()).flatMap(t -> Mono.justOrEmpty( // ✅ CRITICAL
																										// FIX
						rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(), t.highestOi(),
								t.atp(), 60))))
				.doOnNext(rsi -> {

					latestRsi.put(rsi.getSecurityId(), rsi);

					System.out.println("RSI :: " + rsi);

				}).publish().refCount(1);

		// =========================================================
		// 3️⃣ Aggregation (side-effect safe)
		// =========================================================
		Mono<Void> aggregation = rsiFlux.flatMap(r -> Mono.fromRunnable(() -> aggregator.add(r, 60))).then();

		// =========================================================
		// 4️⃣ Decision every candle close
		// =========================================================
		Mono<Void> finalDecision = Flux.interval(Duration.ofSeconds(60)).publishOn(Schedulers.single())
				.flatMap(tick -> {

					// =========================================
					// 1️⃣ Fetch active transactions (BLOCKING → WRAPPED)
					// =========================================
					Mono<Optional<OptionTransaction>> callTxnMono = Mono.fromCallable(
							() -> transactionRepository.findByTimeframeAndOptionTypeAndActive("1M", "CALL", true))
							.subscribeOn(Schedulers.boundedElastic());

					Mono<Optional<OptionTransaction>> putTxnMono = Mono.fromCallable(
							() -> transactionRepository.findByTimeframeAndOptionTypeAndActive("1M", "PUT", true))
							.subscribeOn(Schedulers.boundedElastic());

					// =========================================
					// 2️⃣ Resolve CALL & PUT IDs and Transactions
					// =========================================
					return Mono.zip(callTxnMono, putTxnMono).flatMap(tuple -> {

						Optional<OptionTransaction> callOpt = tuple.getT1();
						Optional<OptionTransaction> putOpt = tuple.getT2();

						// --- CALL ID Mono ---
						Mono<Integer> callIdMono = callOpt.map(o -> Mono.just(o.getSecurityId()))
								.orElseGet(() -> Mono.fromSupplier(() ->
						        latestRsi.values().stream()
						            .filter(r -> r.getOptionType().equalsIgnoreCase("CALL"))
						            .filter(r -> r.getAtp() >= 40 && r.getAtp() <= 45)
						            .max(Comparator.comparing(OptionRsi::getCallFlow))
						            .map(OptionRsi::getSecurityId)
						            .orElse(62580)
						    ));

						// --- PUT ID Mono ---
						Mono<Integer> putIdMono = putOpt.map(o -> Mono.just(o.getSecurityId()))
								.orElseGet(() -> Mono.fromSupplier(() ->
						        latestRsi.values().stream()
						            .filter(r -> r.getOptionType().equalsIgnoreCase("PUT"))
						            .filter(r -> r.getAtp() >= 40 && r.getAtp() <= 45)
						            .max(Comparator.comparing(OptionRsi::getCallFlow))
						            .map(OptionRsi::getSecurityId)
						            .orElse(62380)
						    ));

						// --- Ensure transactions exist ---
						Mono<OptionTransaction> callTransactionMono = ensureTransaction(callOpt, "CALL", callIdMono);
						Mono<OptionTransaction> putTransactionMono = ensureTransaction(putOpt, "PUT", putIdMono);

						// =========================================
						// 3️⃣ Combine CALL & PUT IDs and Transactions
						// =========================================
						return Mono.zip(callIdMono, putIdMono, callTransactionMono, putTransactionMono)
								.flatMap(tuples -> {

									int callId = tuples.getT1();
									int putId = tuples.getT2();

									OptionTransaction callTxn = tuples.getT3();
									OptionTransaction putTxn = tuples.getT4();

									var snap = aggregator.snapshot(60);
									log.info("snap :: {}", snap);

									OptionRsi call = latestRsi.get(callId);
									OptionRsi put = latestRsi.get(putId);

									if (call == null || put == null)
										return Mono.empty();

									// =========================================
									// 4️⃣ Copy Objects
									// =========================================
									OptionRsi callSave = new OptionRsi(call);
									OptionRsi putSave = new OptionRsi(put);

									callSave.setBuy(false);
									callSave.setSell(false);
									putSave.setBuy(false);
									putSave.setSell(false);

									double callFlow = snap.call();
									double putFlow = snap.put();

									double callDelta = call.getDeltaRsi();
									double putDelta = put.getDeltaRsi();

									FlowSignal callSignal = signalService.evaluate(callFlow, putFlow, callDelta,
											"CALL");
									FlowSignal putSignal = signalService.evaluate(callFlow, putFlow, putDelta, "PUT");

									log.info(
											"Decision → callFlow={} putFlow={} callΔ={} putΔ={} callSignal={} putSignal={}",
											callFlow, putFlow, callDelta, putDelta, callSignal, putSignal);

									callSave.setCallFlow(callFlow);
									callSave.setPutFlow(putFlow);
									callSave.setNetFlow(snap.net());

									putSave.setCallFlow(callFlow);
									putSave.setPutFlow(putFlow);
									putSave.setNetFlow(snap.net());

									// =========================================
									// 5️⃣ EXECUTE ORDERS (SIDE EFFECT)
									// =========================================
									executeCallSignal(callSignal, callSave, Optional.of(callTxn));
									executePutSignal(putSignal, putSave, Optional.of(putTxn));

									// =========================================
									// 6️⃣ SAVE DATA
									// =========================================
									List<OptionRsi> list = List.of(callSave, putSave);
									aggregator.reset(60);

									return Mono.fromCallable(() -> rsiRepository.saveAll(list))
											.subscribeOn(Schedulers.boundedElastic()).then();
								});
					});
				}).then();

		// =====================================================
		// 5️⃣ Heartbeat
		// =====================================================
		Mono<Void> heartbeat = session
				.send(Flux.interval(Duration.ofSeconds(15)).map(i -> session.pingMessage(f -> f.allocateBuffer(0))));

		// =====================================================
		// 6️⃣ Lifecycle
		// =====================================================
		return resubscribe.then(Mono.when(aggregation, finalDecision, heartbeat).takeUntilOther(session.closeStatus()))
				.doFinally(s -> this.session = null);
	}

	// =========================================
	// Helper Method
	// =========================================

	private Mono<OptionTransaction> ensureTransaction(Optional<OptionTransaction> optTxn, String type,
			Mono<Integer> idMono) {
		if (optTxn.isPresent()) {
			return Mono.just(optTxn.get());
		} else {
			return idMono.flatMap(id -> Mono
					.fromCallable(() -> transactionRepository.save(OptionTransaction.builder().active(true)
							.optionType(type).timeframe("1M").securityId(id).build()))
					.subscribeOn(Schedulers.boundedElastic()));
		}
	}

	private void executeCallSignal(FlowSignal signal, OptionRsi callSave, Optional<OptionTransaction> callOptFinal) {

		switch (signal) {

		case BUY_CALL -> {
			callSave.setBuy(true);
			placeCallOrder(buildRequest("BUY", callSave));
		}

		case SELL_CALL -> {
			callSave.setSell(true);
			exitCall(buildRequest("SELL", callSave));
			if (!(callSave.getAtp() >= 40 && callSave.getAtp() <= 45)) {
				OptionTransaction optionTransaction = new OptionTransaction();
				if (callOptFinal.isPresent()) {
					optionTransaction = callOptFinal.get();
					optionTransaction.setActive(false);
				}
				transactionRepository.save(optionTransaction);
			}
		}

		default -> {
		}
		}
	}

	private void executePutSignal(FlowSignal signal, OptionRsi putSave, Optional<OptionTransaction> putOptFinal) {

		switch (signal) {

		case BUY_PUT -> {
			putSave.setBuy(true);
			placePutOrder(buildRequest("BUY", putSave));
		}

		case SELL_PUT -> {
			putSave.setSell(true);
			exitPut(buildRequest("SELL", putSave));
			if (!(putSave.getAtp() >= 40 && putSave.getAtp() <= 45)) {
				OptionTransaction optionTransaction = new OptionTransaction();
				if (putOptFinal.isPresent()) {
					optionTransaction = putOptFinal.get();
					optionTransaction.setActive(false);
				}
				transactionRepository.save(optionTransaction);
			}
		}

		default -> {
		}
		}
	}

	private DhanOrderRequest buildRequest(String type, OptionRsi rsi) {

//		FundLimitResponse fund = dhanFundLimitService.getFundLimit()
//	            .subscribeOn(Schedulers.boundedElastic()) // safer thread
//	            .block();
//
//	    if (fund == null) {
//	        throw new RuntimeException("Failed to fetch fund limit");
//	    }
//
//	    double availableBalance = fund.getAvailabelBalance();
//
//	    if (availableBalance < 1000) {
//	        throw new RuntimeException("Insufficient balance");
//	    }
	    
		DhanOrderRequest request = DhanOrderRequest.builder().dhanClientId(clientId)
				.correlationId(CorrelationIdGenerator.generate("NIFTY")).transactionType(type)
				.exchangeSegment("NSE_FNO").productType("MARGIN").orderType("MARKET").validity("DAY")
				.securityId(String.valueOf(rsi.getSecurityId())).quantity(65).disclosedQuantity(0).price(0).triggerPrice(0)
				.afterMarketOrder(false).build();

		try {
			ObjectMapper mapper = new ObjectMapper();
			log.info("DHAN ORDER REQUEST => {}", mapper.writeValueAsString(request));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return request;
	}
	
//	private Mono<DhanOrderRequest> buildRequest(String type, OptionRsi data) {
//
//	    return dhanFundLimitService.getFundLimit()
//	        .map(fund -> {
//
//	            double availableBalance = fund.getAvailabelBalance();
//
//	            // 👉 you can add validation here
//	            if (availableBalance < 1000) {
//	                throw new RuntimeException("Insufficient balance");
//	            }
//
//	            DhanOrderRequest request = DhanOrderRequest.builder()
//	                    .dhanClientId(clientId)
//	                    .correlationId(CorrelationIdGenerator.generate("NIFTY"))
//	                    .transactionType(type)
//	                    .exchangeSegment("NSE_FNO")
//	                    .productType("MARGIN")
//	                    .orderType("MARKET")
//	                    .validity("DAY")
//	                    .securityId(String.valueOf(data.getSecurityId()))
//	                    .quantity(65)
//	                    .disclosedQuantity(0)
//	                    .price(0)
//	                    .triggerPrice(0)
//	                    .afterMarketOrder(false)
//	                    .build();
//
//	            try {
//	                ObjectMapper mapper = new ObjectMapper();
//	                log.info("DHAN ORDER REQUEST => {}", mapper.writeValueAsString(request));
//	            } catch (Exception e) {
//	                log.error("Error serializing request", e);
//	            }
//
//	            return request;
//	        });
//	}

	private void placeCallOrder(DhanOrderRequest request) {

		System.out.println("🟢 BUY CALL");

		dhanOrderService.placeOrder(accessToken, request)
				.doOnSuccess(response -> System.out.println("BUY CALL :: " + response))
				.doOnError(error -> System.out.println("❌ BUY CALL ERROR :: " + error.getMessage())).subscribe();
	}

	private void placePutOrder(DhanOrderRequest request) {

		System.out.println("🔴 BUY PUT");

		dhanOrderService.placeOrder(accessToken, request)
				.doOnSuccess(response -> System.out.println("BUY PUT :: " + response))
				.doOnError(error -> System.out.println("❌ BUY PUT ERROR :: " + error.getMessage())).subscribe();
	}

	private void exitCall(DhanOrderRequest request) {

		System.out.println("⚪ EXIT CALL");

		dhanOrderService.placeOrder(accessToken, request)
				.doOnSuccess(response -> System.out.println("SELL CALL :: " + response))
				.doOnError(error -> System.out.println("❌ SELL CALL ERROR :: " + error.getMessage())).subscribe();
	}

	private void exitPut(DhanOrderRequest request) {

		System.out.println("⚪ EXIT PUT");

		dhanOrderService.placeOrder(accessToken, request)
				.doOnSuccess(response -> System.out.println("SELL PUT :: " + response))
				.doOnError(error -> System.out.println("❌ SELL PUT ERROR :: " + error.getMessage())).subscribe();
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

		String optionType = store.optionTypeOf(securityId);

		// log.info("optionType::ltp {} {}", optionType, ltp);

		if (optionType == null)
			return null;

		return new Tick(securityId, ltp, oi, highestOi, atp, optionType);
	}

	// ================= SUBSCRIBE =================

	public Mono<Void> subscribe(String exchange, String securityId, String optionType, double strike) {

		// 1️⃣ Persist + cache metadata
		store.add(exchange, securityId, optionType, strike);

		// 2️⃣ If WS not connected, DB restore will handle later
		if (session == null || !session.isOpen()) {
			return Mono.empty();
		}

		// 3️⃣ Live subscribe
		return sendSubscription(exchange, securityId);
	}

	private Mono<Void> sendAllSubscriptions() {
		//.filter(entry->entry.getStrike()>=20000 && entry.getStrike()<=30000)
		return Flux.fromIterable(store.all()).flatMap(s -> sendSubscription(s.getExchange(), s.getSecurityId())).then();
	}

	private Mono<Void> sendSubscription(String exchange, String securityId) {

		String payload = """
				{
				  "RequestCode": 21,
				  "InstrumentCount": 1,
				  "InstrumentList": [
				    { "ExchangeSegment": "%s", "SecurityId": "%s" }
				  ]
				}
				""".formatted(exchange, securityId);

		return session.send(Mono.just(session.textMessage(payload)));
	}
}
