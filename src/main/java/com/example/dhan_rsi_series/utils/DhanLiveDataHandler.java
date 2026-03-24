package com.example.dhan_rsi_series.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.example.dhan_rsi_series.entity.OptionRsi;
import com.example.dhan_rsi_series.enums.FlowSignal;
import com.example.dhan_rsi_series.model.DhanOrderRequest;
import com.example.dhan_rsi_series.model.Tick;
import com.example.dhan_rsi_series.repository.OptionRsiRepository;
import com.example.dhan_rsi_series.service.CandleRsiService;
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
	private OptionRsiRepository repository;
	private final Map<Integer, OptionRsi> latestRsi = new ConcurrentHashMap<>();

	@Value("${dhan.client-id}")
    private String clientId;
	
	@Value("${dhan.access-token}")
    private String accessToken;
	
	private final DhanOrderService dhanOrderService;
	public DhanLiveDataHandler(CandleRsiService rsiService, DhanSubscriptionStore store,
			DpiAggregatorService aggregator, FlowSignalService signalService, OptionRsiRepository repository, DhanOrderService dhanOrderService) {
		this.rsiService = rsiService;
		this.store = store;
		this.signalService = signalService;
		this.aggregator = aggregator;
		this.repository = repository;
		this.dhanOrderService = dhanOrderService;
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {

		this.session = session;

		int callId = 62570;
		int putId = 62406;

		Mono<Void> resubscribe = sendAllSubscriptions();

		// =========================================================
		// 1️⃣ Tick stream (safe decode)
		// =========================================================
		Flux<Tick> ticks = session.receive().filter(m -> m.getType() == WebSocketMessage.Type.BINARY)
				.mapNotNull(m -> decode(m.getPayload())) // ✅ null safe
				.filter(m->m.atp()>=8 && m.atp()<=325)
				.share();

		// =========================================================
		// 2️⃣ RSI stream (PARALLEL + NULL SAFE)
		// =========================================================
		Flux<OptionRsi> rsiFlux = ticks.groupBy(Tick::securityId)
				.flatMap(group -> group.publishOn(Schedulers.parallel()).flatMap(t -> Mono.justOrEmpty( // ✅ CRITICAL
																										// FIX
						rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(), t.highestOi(),
								t.atp(), 30))))
				.doOnNext(rsi ->{

                    latestRsi.put(rsi.getSecurityId(), rsi);

                    System.out.println("RSI :: " + rsi);

                } ).publish().refCount(1);



		// =========================================================
		// 3️⃣ Aggregation (side-effect safe)
		// =========================================================
		Mono<Void> aggregation = rsiFlux.flatMap(r -> Mono.fromRunnable(() -> aggregator.add(r, 30))).then();

		// =========================================================
		// 4️⃣ Decision every candle close
		// =========================================================
		Mono<Void> decision = Flux.interval(Duration.ofSeconds(30))
				.publishOn(Schedulers.single())
				.flatMap(i -> {

				    var snap = aggregator.snapshot(30);

				    log.info("snap :: {}", snap);
				    
				    
				    
				    //OptionRsi call = rsiService.latest(callId);
				    //OptionRsi put  = rsiService.latest(putId);
				    
				    OptionRsi call = latestRsi.get(callId);
                    OptionRsi put  = latestRsi.get(putId);

				    if (call == null || put == null)
				        return Mono.empty();

				    // ------------------------------------------
                    // Copy objects (never mutate cached objects)
                    // ------------------------------------------
                    OptionRsi callSave = new OptionRsi(call);
                    OptionRsi putSave  = new OptionRsi(put);

                    // reset flags
                    callSave.setBuy(false);
                    callSave.setSell(false);

                    putSave.setBuy(false);
                    putSave.setSell(false);

                    double callFlow = snap.call();
                    double putFlow  = snap.put();

                    double callDelta = call.getDeltaRsi();
                    double putDelta  = put.getDeltaRsi();

                    FlowSignal callSignal =
                            signalService.evaluate(
                                    callFlow,
                                    putFlow,
                                    callDelta,
                                    "CALL"
                            );

                    FlowSignal putSignal =
                            signalService.evaluate(
                                    callFlow,
                                    putFlow,
                                    putDelta,
                                    "PUT"
                            );

                    System.out.println(
                            "Decision → callFlow=" + callFlow +
                            " putFlow=" + putFlow +
                            " callΔ=" + callDelta +
                            " putΔ=" + putDelta +
                            " callSignal=" + callSignal +
                            " putSignal=" + putSignal
                    );

                    callSave.setCallFlow(callFlow);
                    callSave.setPutFlow(putFlow);
                    callSave.setNetFlow(snap.net());

                    putSave.setCallFlow(callFlow);
                    putSave.setPutFlow(putFlow);
                    putSave.setNetFlow(snap.net());

                    // ======================================
                    // CALL SIGNAL
                    // ======================================
                    switch (callSignal) {

                        case BUY_CALL -> {
                        	
                        	DhanOrderRequest request = DhanOrderRequest.builder()
                        	        .dhanClientId(clientId)
                        	        .correlationId(CorrelationIdGenerator.generate("NIFTY"))
                        	        .transactionType("BUY")
                        	        .exchangeSegment("NSE_FNO")
                        	        .productType("INTRADAY")
                        	        .orderType("MARKET")
                        	        .validity("DAY")
                        	        .securityId(String.valueOf(callSave.getSecurityId()))
                        	        .quantity(65)
                        	        .disclosedQuantity(0)
                        	        .price(0)
                        	        .triggerPrice(0)
                        	        .afterMarketOrder(false)
                        	        .build();
                        	
                            callSave.setBuy(true);
                            
                            // 🔍 PRINT REQUEST BODY
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                String json = mapper.writeValueAsString(request);
                                System.out.println("DHAN ORDER REQUEST => " + json);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            
                            placeCallOrder(request);
                        }

                        case SELL_CALL -> {
                        	DhanOrderRequest request = DhanOrderRequest.builder()
                        	        .dhanClientId(clientId)
                        	        .correlationId(CorrelationIdGenerator.generate("NIFTY"))
                        	        .transactionType("SELL")
                        	        .exchangeSegment("NSE_FNO")
                        	        .productType("INTRADAY")
                        	        .orderType("MARKET")
                        	        .validity("DAY")
                        	        .securityId(String.valueOf(callSave.getSecurityId()))
                        	        .quantity(65)
                        	        .disclosedQuantity(0)
                        	        .price(0)
                        	        .triggerPrice(0)
                        	        .afterMarketOrder(false)
                        	        .build();
                        	
                        	 // 🔍 PRINT REQUEST BODY
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                String json = mapper.writeValueAsString(request);
                                System.out.println("DHAN ORDER REQUEST => " + json);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        	
                            callSave.setSell(true);
                            exitCall(request);
                        }

                        default -> {}
                    }

                    // ======================================
                    // PUT SIGNAL
                    // ======================================
                    switch (putSignal) {

                        case BUY_PUT -> {
                        	DhanOrderRequest request = DhanOrderRequest.builder()
                        	        .dhanClientId(clientId)
                        	        .correlationId(CorrelationIdGenerator.generate("NIFTY"))
                        	        .transactionType("BUY")
                        	        .exchangeSegment("NSE_FNO")
                        	        .productType("INTRADAY")
                        	        .orderType("MARKET")
                        	        .validity("DAY")
                        	        .securityId(String.valueOf(putSave.getSecurityId()))
                        	        .quantity(65)
                        	        .disclosedQuantity(0)
                        	        .price(0)
                        	        .triggerPrice(0)
                        	        .afterMarketOrder(false)
                        	        .build();
                            putSave.setBuy(true);
                            
                            // 🔍 PRINT REQUEST BODY
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                String json = mapper.writeValueAsString(request);
                                System.out.println("DHAN ORDER REQUEST => " + json);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            placePutOrder(request);
                        }

                        case SELL_PUT -> {
                        	DhanOrderRequest request = DhanOrderRequest.builder()
                        	        .dhanClientId(clientId)
                        	        .correlationId(CorrelationIdGenerator.generate("NIFTY"))
                        	        .transactionType("SELL")
                        	        .exchangeSegment("NSE_FNO")
                        	        .productType("INTRADAY")
                        	        .orderType("MARKET")
                        	        .validity("DAY")
                        	        .securityId(String.valueOf(putSave.getSecurityId()))
                        	        .quantity(65)
                        	        .disclosedQuantity(0)
                        	        .price(0)
                        	        .triggerPrice(0)
                        	        .afterMarketOrder(false)
                        	        .build();
                        	
                        	 // 🔍 PRINT REQUEST BODY
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                String json = mapper.writeValueAsString(request);
                                System.out.println("DHAN ORDER REQUEST => " + json);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        	
                            putSave.setSell(true);
                            exitPut(request);
                        }

                        default -> {}
                    }

                    List<OptionRsi> list =
                            List.of(callSave, putSave);

                    aggregator.reset(30);

                    return Mono.fromCallable(() ->
                            repository.saveAll(list)
                    )
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();

                })
                .then();

    // =====================================================
    // 5️⃣ Heartbeat
    // =====================================================
    Mono<Void> heartbeat =
            session.send(
                    Flux.interval(Duration.ofSeconds(15))
                        .map(i ->
                            session.pingMessage(
                                    f -> f.allocateBuffer(0)
                            )
                        )
            );

    // =====================================================
    // 6️⃣ Lifecycle
    // =====================================================
    return resubscribe
            .then(
                    Mono.when(
                            aggregation,
                            decision,
                            heartbeat
                    )
                    .takeUntilOther(session.closeStatus())
            )
            .doFinally(s -> this.session = null);
}

	private void placeCallOrder(DhanOrderRequest request) {

	    System.out.println("🟢 BUY CALL");

	    dhanOrderService.placeOrder(accessToken, request)
	            .doOnSuccess(response ->
	                    System.out.println("BUY CALL :: " + response))
	            .doOnError(error ->
	                    System.out.println("❌ BUY CALL ERROR :: " + error.getMessage()))
	            .subscribe();
	}

	private void placePutOrder(DhanOrderRequest request) {

	    System.out.println("🔴 BUY PUT");

	    dhanOrderService.placeOrder(accessToken, request)
	            .doOnSuccess(response ->
	                    System.out.println("BUY PUT :: " + response))
	            .doOnError(error ->
	                    System.out.println("❌ BUY PUT ERROR :: " + error.getMessage()))
	            .subscribe();
	}

	private void exitCall(DhanOrderRequest request) {

	    System.out.println("⚪ EXIT CALL");

	    dhanOrderService.placeOrder(accessToken, request)
	            .doOnSuccess(response ->
	                    System.out.println("SELL CALL :: " + response))
	            .doOnError(error ->
	                    System.out.println("❌ SELL CALL ERROR :: " + error.getMessage()))
	            .subscribe();
	}

	private void exitPut(DhanOrderRequest request) {

	    System.out.println("⚪ EXIT PUT");

	    dhanOrderService.placeOrder(accessToken, request)
	            .doOnSuccess(response ->
	                    System.out.println("SELL PUT :: " + response))
	            .doOnError(error ->
	                    System.out.println("❌ SELL PUT ERROR :: " + error.getMessage()))
	            .subscribe();
	}

	// =====================================================
	// ================= DECODE =================
	// =====================================================

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

	public Mono<Void> subscribe(String exchange, String securityId, String optionType) {

		// 1️⃣ Persist + cache metadata
		store.add(exchange, securityId, optionType);

		// 2️⃣ If WS not connected, DB restore will handle later
		if (session == null || !session.isOpen()) {
			return Mono.empty();
		}

		// 3️⃣ Live subscribe
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
				    { "ExchangeSegment": "%s", "SecurityId": "%s" }
				  ]
				}
				""".formatted(exchange, securityId);

		return session.send(Mono.just(session.textMessage(payload)));
	}
}
