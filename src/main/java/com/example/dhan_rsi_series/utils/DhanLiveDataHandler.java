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

/*@Component
public class DhanLiveDataHandler implements WebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final int RSI_PERIOD = 14;
    private static final long CANDLE_DURATION_MS = 5_000;
    
    // ================= STATE =================

    private Candle currentCandle;
    private long candleStartTime = -1;

    private final Deque<Double> closePrices = new ArrayDeque<>();

    private Double avgGain = null;
    private Double avgLoss = null;
    
 // ================= SOCKET =================

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        String subscribe = """
        {
          "RequestCode": 15,
          "InstrumentCount": 1,
          "InstrumentList": [
            {
              "ExchangeSegment": "NSE_FNO",
              "SecurityId": "58650"
            }
          ]
        }
        """;

        Mono<Void> send = session.send(
            Mono.just(session.textMessage(subscribe))
        );

        Mono<Void> receive = session.receive()
            .doOnNext(msg -> {
                if (msg.getType() == WebSocketMessage.Type.BINARY) {
                    handleBinary(msg.getPayload());
                }
            })
            .doOnComplete(() ->
                System.out.println("WebSocket closed by server")
            )
            .then();

        Mono<Void> heartbeat = Flux.interval(Duration.ofSeconds(10))
            .flatMap(i ->
                session.send(
                    Mono.just(session.pingMessage(db -> db.allocateBuffer()))
                )
            )
            .then();

        return Mono.when(send, receive, heartbeat);
    }

    // ================= BINARY =================

    private void handleBinary(DataBuffer buffer) {

        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);

        System.out.println(
            "RAW(hex) → " + HexFormat.of().formatHex(bytes)
        );

        decodeNseFnoTicker(bytes);
    }

    // ================= NSE_FNO DECODER =================

    private void decodeNseFnoTicker(byte[] bytes) {

        ByteBuffer bb = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN);

        // Minimum NSE_FNO tick size
        if (bb.remaining() < 12) return;

        short messageType = bb.getShort();      // 2 bytes
        short exchangeSeg = bb.getShort();      // 2 bytes
        int securityId    = bb.getInt();        // 4 bytes
        float ltp         = bb.getFloat();      // 4 bytes
        
        //processTick(ltp);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", "NSE_FNO_TICK");
        json.put("messageType", messageType);
        json.put("exchangeSegment", exchangeSeg);
        json.put("securityId", securityId);
        json.put("ltp", ltp);
        json.put("timestamp", System.currentTimeMillis());

        try {
            System.out.println(
                mapper.writerWithDefaultPrettyPrinter()
                      .writeValueAsString(json)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processTick(double ltp) {

        long now = System.currentTimeMillis();

        if (candleStartTime == -1) {
            startNewCandle(now, ltp);
            return;
        }

        if (now - candleStartTime < CANDLE_DURATION_MS) {
            currentCandle.update(ltp);
        } else {
            closeCandle();
            startNewCandle(now, ltp);
        }
    }

    private void startNewCandle(long time, double price) {
        candleStartTime = time;
        currentCandle = new Candle(price);
    }

    private void closeCandle() {

        double close = currentCandle.close;
        closePrices.addLast(close);

        if (closePrices.size() > RSI_PERIOD) {
            closePrices.removeFirst();
        }

        if (closePrices.size() == RSI_PERIOD) {
            double rsi = calculateRSI(close);
            printRsi(close, rsi);
        }
    }

    // ================= RSI =================

    private double calculateRSI(double latestClose) {

        if (avgGain == null) {
            initializeRsi();
        } else {
            double prevClose = getPreviousClose();
            double change = latestClose - prevClose;

            double gain = Math.max(change, 0);
            double loss = Math.max(-change, 0);

            avgGain = (avgGain * (RSI_PERIOD - 1) + gain) / RSI_PERIOD;
            avgLoss = (avgLoss * (RSI_PERIOD - 1) + loss) / RSI_PERIOD;
        }

        if (avgLoss == 0) return 100;

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    private void initializeRsi() {

        Iterator<Double> it = closePrices.iterator();
        double prev = it.next();

        double gain = 0, loss = 0;

        while (it.hasNext()) {
            double curr = it.next();
            double diff = curr - prev;

            if (diff > 0) gain += diff;
            else loss -= diff;

            prev = curr;
        }

        avgGain = gain / RSI_PERIOD;
        avgLoss = loss / RSI_PERIOD;
    }

    private double getPreviousClose() {
        Iterator<Double> it = closePrices.descendingIterator();
        it.next();
        return it.next();
    }

    // ================= OUTPUT =================

    private void printRsi(double close, double rsi) {

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("candleDuration", "5s");
        json.put("close", close);
        json.put("rsiPeriod", RSI_PERIOD);
        json.put("rsi", Math.round(rsi * 100.0) / 100.0);
        json.put("timestamp", System.currentTimeMillis());

        try {
            System.out.println(
                    mapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(json)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= MODEL =================

    static class Candle {
        double open, high, low, close;

        Candle(double price) {
            open = high = low = close = price;
        }

        void update(double price) {
            close = price;
            high = Math.max(high, price);
            low = Math.min(low, price);
        }
    }
}*/

/*@Component
public class DhanLiveDataHandler implements WebSocketHandler {

    @Autowired
    private RsiRepository repo;

    private final Map<Integer, RsiProcessor> processors = new HashMap<>();

    @PostConstruct
    void init() {
        processors.put(58711, new RsiProcessor("58711", "CALL", repo));
        processors.put(58650, new RsiProcessor("58650", "PUT", repo));
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        String subscribe = """
        {
          "RequestCode": 21,
          "InstrumentCount": 2,
          "InstrumentList": [
            { "ExchangeSegment": "NSE_FNO", "SecurityId": "58711" },
            { "ExchangeSegment": "NSE_FNO", "SecurityId": "58650" }
          ]
        }
        """;

        Mono<Void> send = session.send(
            Mono.just(session.textMessage(subscribe))
        );

        Mono<Void> receive = session.receive()
            .doOnNext(msg -> {
                if (msg.getType() == WebSocketMessage.Type.BINARY) {
                    processBinary(msg.getPayload());
                }
            })
            .then();

        Mono<Void> heartbeat = Flux.interval(Duration.ofSeconds(10))
            .flatMap(i ->
                session.send(
                    Mono.just(session.pingMessage(db -> db.allocateBuffer()))
                )
            )
            .then();

        return Mono.when(send, receive, heartbeat);
    }

    private void processBinary(DataBuffer buffer) {

        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);

        ByteBuffer bb = ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN);

        if (bb.remaining() < 12) return;

        bb.getShort(); // messageType
        bb.getShort(); // exchange
        int securityId = bb.getInt();
        float ltp = bb.getFloat();

        RsiProcessor processor = processors.get(securityId);
        if (processor != null) {
            processor.onTick(ltp);
        }
    }
}*/

/*@Component
public class DhanLiveDataHandler implements WebSocketHandler {

    private final CandleRsiService rsiService;

    public DhanLiveDataHandler(CandleRsiService rsiService) {
        this.rsiService = rsiService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        String subscribe = """
        {
          "RequestCode": 15,
          "InstrumentCount": 2,
          "InstrumentList": [
            { "ExchangeSegment": "NSE_FNO", "SecurityId": "58650" },
            { "ExchangeSegment": "NSE_FNO", "SecurityId": "58711" }
          ]
        }
        """;

        Mono<Void> send = session.send(
            Mono.just(session.textMessage(subscribe))
        );

        Mono<Void> receive = session.receive()
            .filter(msg -> msg.getType() == WebSocketMessage.Type.BINARY)
            .doOnNext(msg -> decode(msg.getPayload()))
            .then();

        Mono<Void> heartbeat = Flux.interval(Duration.ofSeconds(10))
            .flatMap(i -> session.send(
                Mono.just(session.pingMessage(
                    db -> db.allocateBuffer()))
            ))
            .then();

        return Mono.when(send, receive, heartbeat);
    }

    // ================= BINARY DECODER =================

    private void decode(DataBuffer buffer) {

        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        
        System.out.println("Live:: "+bytes);

        ByteBuffer bb = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN);

        if (bb.remaining() < 12) return;

        short msgType = bb.getShort();
        short exchange = bb.getShort();
        int securityId = bb.getInt();
        float ltp = bb.getFloat();

        String optionType =
                (securityId == 58650) ? "PUT" : "CALL";

        // 🔥 5-second candle
        rsiService.onLtp(
            "NIFTY", securityId,
            optionType, ltp, 5
        );

        // 🔥 1-minute candle
        rsiService.onLtp(
            "NIFTY", securityId,
            optionType, ltp, 60
        );
    }
}*/

/*@Component
public class DhanLiveDataHandler implements WebSocketHandler {

    private final CandleRsiService rsiService;

    public DhanLiveDataHandler(CandleRsiService rsiService) {
        this.rsiService = rsiService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        String subscribe = """
        {
          "RequestCode": 15,
          "InstrumentCount": 2,
          "InstrumentList": [
            { "ExchangeSegment": "NSE_FNO", "SecurityId": "58650" },
            { "ExchangeSegment": "NSE_FNO", "SecurityId": "58711" }
          ]
        }
        """;

        Flux<WebSocketMessage> outbound = Flux.concat(
        	    Mono.just(session.textMessage(subscribe)),
        	    Flux.interval(Duration.ofSeconds(10))
        	        .map(i -> session.pingMessage(f ->
        	            f.allocateBuffer(0)))
        	);


        Mono<Void> inbound = session.receive()
            .filter(msg -> msg.getType() == WebSocketMessage.Type.BINARY)
            .doOnNext(msg -> decode(msg.getPayload()))
            .doFinally(sig ->
                System.out.println("WebSocket session ended: " + sig))
            .then();

        return session.send(outbound).and(inbound);
    }

    private void decode(DataBuffer buffer) {
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);

        ByteBuffer bb = ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN);

        if (bb.remaining() < 12) return;

        bb.getShort(); // msgType
        bb.getShort(); // exchange
        int securityId = bb.getInt();
        float ltp = bb.getFloat();

        String optionType = (securityId == 58650) ? "PUT" : "CALL";

        rsiService.onLtp("NIFTY", securityId, optionType, ltp, 5);
        rsiService.onLtp("NIFTY", securityId, optionType, ltp, 60);
    }
}*/

/*@Component
public class DhanLiveDataHandler implements WebSocketHandler {

    private final CandleRsiService rsiService;
    private final DhanSubscriptionStore store;

    private volatile WebSocketSession session;

    public DhanLiveDataHandler(
            CandleRsiService rsiService,
            DhanSubscriptionStore store) {

        this.rsiService = rsiService;
        this.store = store;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        this.session = session;

        Mono<Void> resubscribe = sendAllSubscriptions();

        Mono<Void> receive = session.receive()
            .filter(m -> m.getType() == WebSocketMessage.Type.BINARY)
            .doOnNext(m -> decode(m.getPayload()))
            .doFinally(sig -> this.session = null)
            .then();

        Mono<Void> heartbeat =
            Flux.interval(Duration.ofSeconds(15))
                .takeUntilOther(receive)
                .flatMap(i ->
                    session.send(
                        Mono.just(
                            session.pingMessage(
                                factory -> factory.allocateBuffer(0)
                            )
                        )
                    )
                )
                .then();

        return Mono.when(resubscribe, receive, heartbeat);
    }

    // ================= SUBSCRIBE =================

    public Mono<Void> subscribe(String exchange, String securityId) {

        store.add(exchange, securityId);

        if (session == null || !session.isOpen()) {
            return Mono.empty();
        }

        return sendSubscription(exchange, securityId);
    }

    private Mono<Void> sendAllSubscriptions() {

        if (store.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(store.snapshot())
            .flatMap(key -> {
                String[] p = key.split("\\|");
                return sendSubscription(p[0], p[1]);
            })
            .then();
    }

    private Mono<Void> sendSubscription(String exchange, String securityId) {

        String payload = """
        {
          "RequestCode": 15,
          "InstrumentCount": 1,
          "InstrumentList": [
            { "ExchangeSegment": "%s", "SecurityId": "%s" }
          ]
        }
        """.formatted(exchange, securityId);

        return session.send(
            Mono.just(session.textMessage(payload))
        );
    }

    // ================= BINARY DECODER =================

    private void decode(DataBuffer buffer) {

        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);

        ByteBuffer bb = ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN);

        if (bb.remaining() < 12) return;

        bb.getShort(); // msgType
        bb.getShort(); // exchange
        int securityId = bb.getInt();
        float ltp = bb.getFloat();

        String optionType =
            (securityId == 58650) ? "PUT" : "CALL";

        rsiService.onLtp("NIFTY", securityId, optionType, ltp, 5);
        rsiService.onLtp("NIFTY", securityId, optionType, ltp, 60);
    }
}*/

/*@Component
public class DhanLiveDataHandler implements WebSocketHandler {

    private final CandleRsiService rsiService;
    private final DhanSubscriptionStore store;

    private volatile WebSocketSession session;

    public DhanLiveDataHandler(
            CandleRsiService rsiService,
            DhanSubscriptionStore store) {

        this.rsiService = rsiService;
        this.store = store;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        this.session = session;

        Mono<Void> resubscribe = sendAllSubscriptions();

        Mono<Void> receive = session.receive()
            .filter(m -> m.getType() == WebSocketMessage.Type.BINARY)
            .doOnNext(m -> decode(m.getPayload()))
            .doFinally(s -> this.session = null)
            .then();

        Mono<Void> heartbeat =
            Flux.interval(Duration.ofSeconds(15))
                .takeUntilOther(receive)
                .flatMap(i ->
                    session.send(
                        Mono.just(
                            session.pingMessage(
                                f -> f.allocateBuffer(0)
                            )
                        )
                    )
                )
                .then();

        return Mono.when(resubscribe, receive, heartbeat);
    }

    // ================= DECODE =================

    private void decode(DataBuffer buffer) {

        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);

        ByteBuffer bb = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN);

        if (bb.remaining() < 12) return;

        bb.getShort();
        bb.getShort();

        int securityId = bb.getInt();
        float ltp = bb.getFloat();

        String optionType =
                (securityId == 58650) ? "PUT" : "CALL";

        rsiService.onLtp("NIFTY", securityId, optionType, ltp, 5);
        rsiService.onLtp("NIFTY", securityId, optionType, ltp, 60);
    }

    // ================= SUBSCRIBE =================

    public Mono<Void> subscribe(String exchange, String securityId) {

        store.add(exchange, securityId);

        if (session == null || !session.isOpen()) {
            return Mono.empty();
        }

        return sendSubscription(exchange, securityId);
    }

    private Mono<Void> sendAllSubscriptions() {

        return Flux.fromIterable(store.snapshot())
            .flatMap(k -> {
                String[] p = k.split("\\|");
                return sendSubscription(p[0], p[1]);
            })
            .then();
    }

    private Mono<Void> sendSubscription(String exchange, String securityId) {

        String payload = """
        {
          "RequestCode": 15,
          "InstrumentCount": 1,
          "InstrumentList": [
            { "ExchangeSegment": "%s", "SecurityId": "%s" }
          ]
        }
        """.formatted(exchange, securityId);

        return session.send(
            Mono.just(session.textMessage(payload))
        );
    }
}*/

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

//    @Override
//    public Mono<Void> handle(WebSocketSession session) {
//
//        this.session = session;
//
//        // 🔁 Restore subscriptions once connected
//        Mono<Void> resubscribe = sendAllSubscriptions();
//
//        // 📥 Receive binary data
//        Mono<Void> inbound =
//            session.receive()
//                .filter(m -> m.getType() == WebSocketMessage.Type.BINARY)
//                .doOnNext(m -> decode(m.getPayload()))
//                .doOnError(e ->
//                    System.err.println("📴 Session closed: onError")
//                )
//                .doFinally(s -> this.session = null)
//                .then();
//
//        // ❤️ Heartbeat (runs forever until socket closes)
//        Flux<WebSocketMessage> heartbeat =
//            Flux.interval(Duration.ofSeconds(15))
//                .map(i ->
//                    session.pingMessage(f -> f.allocateBuffer(0))
//                );
//
//        // ⬆️ Merge heartbeat into outbound stream
//        Mono<Void> outbound =
//            session.send(heartbeat)
//                .doOnError(e ->
//                    System.err.println("📴 Heartbeat stopped")
//                );
//
//        // 🚨 KEEP SOCKET ALIVE
//        return resubscribe
//            .then(Mono.when(inbound, outbound))
//            .then(Mono.never()); // 🔥 THIS keeps it open
//    }

	/*
	 * @Override public Mono<Void> handle(WebSocketSession session) {
	 * 
	 * this.session = session;
	 * 
	 * Mono<Void> resubscribe = sendAllSubscriptions();
	 * 
	 * // ========================================================= // 1️⃣ Tick
	 * stream // =========================================================
	 * Flux<Tick> ticks = session.receive() .filter(m -> m.getType() ==
	 * WebSocketMessage.Type.BINARY) .map(m -> decode(m.getPayload()))
	 * .filter(Objects::nonNull) .share();
	 * 
	 * // ========================================================= // 2️ Parallel
	 * Candle + RSI + DPI //
	 * ========================================================= Flux<OptionRsi>
	 * rsiFlux = ticks .groupBy(Tick::securityId) .flatMap(group -> group
	 * .publishOn(Schedulers.parallel()) .concatMap(t -> Flux.just(
	 * rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(),
	 * t.highestOi(), t.atp(), 30), rsiService.onLtp("NIFTY", t.securityId(),
	 * t.optionType(), t.ltp(), t.oi(), t.highestOi(), t.atp(), 60) ))
	 * .filter(Objects::nonNull) ) .share();
	 * 
	 * // ========================================================= // 3️
	 * Aggregation flow (parallel) //
	 * ========================================================= Mono<Void>
	 * aggregation = rsiFlux .publishOn(Schedulers.parallel()) .doOnNext(r ->
	 * aggregator.add(r, 60)) .then();
	 * 
	 * // ========================================================= // 4️⃣ Decision
	 * flow (single thread) //
	 * ========================================================= Mono<Void> decision
	 * = Flux.interval(Duration.ofSeconds(60)) .publishOn(Schedulers.single())
	 * .doOnNext(i -> {
	 * 
	 * OptionRsi target = rsiService.latestTarget(); // your selected strike
	 * 
	 * var snap = aggregator.snapshot(60);
	 * 
	 * //FlowSignal signal = signalService.evaluate(snap);
	 * 
	 * FlowSignal signal = signalService.evaluate(snap, target);
	 * 
	 * switch (signal) {
	 * 
	 * case BUY_CALL -> placeCallOrder(target); case SELL_CALL -> exitCall(target);
	 * 
	 * case BUY_PUT -> placePutOrder(target); case SELL_PUT -> exitPut(target);
	 * 
	 * default -> {} }
	 * 
	 * aggregator.reset(60); }) .then();
	 * 
	 * // ========================================================= // 5️⃣ Heartbeat
	 * // ========================================================= Mono<Void>
	 * outbound = session.send( Flux.interval(Duration.ofSeconds(15)) .map(i ->
	 * session.pingMessage(f -> f.allocateBuffer(0))) );
	 * 
	 * // ========================================================= // 6️⃣ Lifecycle
	 * (NO subscribe() anywhere) //
	 * ========================================================= return resubscribe
	 * .then(Mono.when(aggregation, decision, outbound)) .doFinally(s ->
	 * this.session = null); }
	 */

//    @Override
//    public Mono<Void> handle(WebSocketSession session) {
//
//        this.session = session;
//
//        int callId = 45553; // your chosen strike
//        int putId  = 45508;
//
//        Mono<Void> resubscribe = sendAllSubscriptions();
//
//        Flux<Tick> ticks =
//                session.receive()
//                       .filter(m -> m.getType() == WebSocketMessage.Type.BINARY)
//                       //.map(m -> decode(m.getPayload()))
//                       .mapNotNull(m -> decode(m.getPayload()))
//                       .filter(Objects::nonNull)
//                       .share();
//
//        Flux<OptionRsi> rsiFlux =
//                ticks
//                    .groupBy(Tick::securityId)
//                    .flatMap(group ->
//                            group.publishOn(Schedulers.parallel())
//                                 .map(t -> rsiService.onLtp(
//                                         "NIFTY",
//                                         t.securityId(),
//                                         t.optionType(),
//                                         t.ltp(),
//                                         t.oi(),
//                                         t.highestOi(),
//                                         t.atp(),
//                                         60
//                                 ))
//                                 .filter(Objects::nonNull)
//                    )
//                    .share();
//
//        // aggregation
//        Mono<Void> aggregation =
//                rsiFlux
//                    .publishOn(Schedulers.parallel())
//                    .doOnNext(r -> aggregator.add(r, 60))
//                    .then();
//
//        // decision every 60s
//        Mono<Void> decision =
//                Flux.interval(Duration.ofSeconds(60))
//                    .publishOn(Schedulers.single())
//                    .doOnNext(i -> {
//
//                        var snap = aggregator.snapshot(60);
//
//                        OptionRsi call = rsiService.latest(callId);
//                        OptionRsi put  = rsiService.latest(putId);
//
//                        FlowSignal signal =
//                                signalService.evaluate(snap, call, put);
//
//                        switch (signal) {
//                            case BUY_CALL  -> {
//                            	call.setBuy(true);
//                            	placeCallOrder();
//                            	}
//                            case SELL_CALL -> {
//                            	call.setSell(true);
//                            	exitCall();
//                            }
//                            case BUY_PUT   -> {
//                            	put.setBuy(true);
//                            	placePutOrder();
//                            }
//                            case SELL_PUT  -> {
//                            	put.setSell(true);
//                            	exitPut();
//                            }
//                            default -> {}
//                        }
//                        
//                        List<OptionRsi> optionRsiList = new ArrayList<>();
//                        
//                        optionRsiList.add(call);
//                        optionRsiList.add(put);
//                        
//                        repository.saveAll(optionRsiList);
//
//                        optionRsiList.clear();
//                        aggregator.reset(60);
//                    })
//                    .then();
//
//        Mono<Void> heartbeat =
//                session.send(
//                        Flux.interval(Duration.ofSeconds(15))
//                            .map(i -> session.pingMessage(f -> f.allocateBuffer(0)))
//                );
//
//        return resubscribe
//                .then(Mono.when(aggregation, decision, heartbeat));
//    }

	@Override
	public Mono<Void> handle(WebSocketSession session) {

		this.session = session;

		int callId = 62609;
		int putId = 62406;

		Mono<Void> resubscribe = sendAllSubscriptions();

		// =========================================================
		// 1️⃣ Tick stream (safe decode)
		// =========================================================
		Flux<Tick> ticks = session.receive().filter(m -> m.getType() == WebSocketMessage.Type.BINARY)
				.mapNotNull(m -> decode(m.getPayload())) // ✅ null safe
				.share();

		// =========================================================
		// 2️⃣ RSI stream (PARALLEL + NULL SAFE)
		// =========================================================
		Flux<OptionRsi> rsiFlux = ticks.groupBy(Tick::securityId)
				.flatMap(group -> group.publishOn(Schedulers.parallel()).flatMap(t -> Mono.justOrEmpty( // ✅ CRITICAL
																										// FIX
						rsiService.onLtp("NIFTY", t.securityId(), t.optionType(), t.ltp(), t.oi(), t.highestOi(),
								t.atp(), 60))))
				.doOnNext(rsi ->{

                    latestRsi.put(rsi.getSecurityId(), rsi);

                    System.out.println("RSI :: " + rsi);

                } ).publish().refCount(1);



		// =========================================================
		// 3️⃣ Aggregation (side-effect safe)
		// =========================================================
		Mono<Void> aggregation = rsiFlux.flatMap(r -> Mono.fromRunnable(() -> aggregator.add(r, 60))).then();

		// =========================================================
		// 4️⃣ Decision every candle close
		// =========================================================
//		Mono<Void> decision = Flux.interval(Duration.ofSeconds(60))
//				.publishOn(Schedulers.single())
//				.flatMap(i -> {
//
//				    var snap = aggregator.snapshot(60);
//
//				    log.info("snap :: {}", snap);
//
//				    OptionRsi call = rsiService.latest(callId);
//				    OptionRsi put  = rsiService.latest(putId);
//
//				    if (call == null || put == null)
//				        return Mono.empty();
//
//				    double net = snap.net();
//				    double callDeltaRsi = call.getDeltaRsi();
//				    double putDeltaRsi = put.getDeltaRsi();
//				    
//				    log.info("net, callDeltaRsi, putDeltaRsi :: {}, {}, {}", net, callDeltaRsi, putDeltaRsi);
//
//				    FlowSignal callSignal =
//				            signalService.evaluate(net, callDeltaRsi, "CALL");
//
//				    FlowSignal putSignal =
//				            signalService.evaluate(net, putDeltaRsi, "PUT");
//				    
//				    log.info("callSignal, putSignal :: {}, {}", callSignal, putSignal);
//
//
//				    call.setCallFlow(snap.call());
//				    call.setPutFlow(snap.put());
//				    call.setNetFlow(snap.net());
//
//				    put.setCallFlow(snap.call());
//				    put.setPutFlow(snap.put());
//				    put.setNetFlow(snap.net());
//
//
//				    switch (callSignal) {
//
//				        case BUY_CALL -> {
//				            call.setBuy(true);
//				            placeCallOrder();
//				        }
//
//				        case SELL_CALL -> {
//				            call.setSell(true);
//				            exitCall();
//				        }
//
//				        default -> {}
//				    }
//
//
//				    switch (putSignal) {
//
//				        case BUY_PUT -> {
//				            put.setBuy(true);
//				            placePutOrder();
//				        }
//
//				        case SELL_PUT -> {
//				            put.setSell(true);
//				            exitPut();
//				        }
//
//				        default -> {}
//				    }
//
//
//				    List<OptionRsi> list = List.of(call, put);
//
//				    aggregator.reset(60);
//
//				    return Mono.fromCallable(() -> repository.saveAll(list))
//				            .subscribeOn(Schedulers.boundedElastic())
//				            .then();
//
//				})
//				.then();
		
		
		Mono<Void> decision = Flux.interval(Duration.ofSeconds(60))
				.publishOn(Schedulers.single())
				.flatMap(i -> {

				    var snap = aggregator.snapshot(60);

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
                        	        .productType("MARGIN")
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
                        	        .productType("MARGIN")
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
                        	        .productType("MARGIN")
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
                        	        .productType("MARGIN")
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

                    aggregator.reset(60);

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

//    @Component
//    public class DhanLiveDataHandler implements WebSocketHandler {
//
//        private final CandleRsiService rsiService;
//        private final DhanSubscriptionStore store;
//        private final DpiAggregatorService aggregator;
//
//        public DhanLiveDataHandler(
//                CandleRsiService rsiService,
//                DhanSubscriptionStore store,
//                DpiAggregatorService aggregator) {
//
//            this.rsiService = rsiService;
//            this.store = store;
//            this.aggregator = aggregator;
//        }

	// =====================================================

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

	// ================= DECODE =================

	/*
	 * private void decode(DataBuffer buffer) {
	 * 
	 * byte[] bytes = new byte[buffer.readableByteCount()]; buffer.read(bytes);
	 * 
	 * ByteBuffer bb = ByteBuffer .wrap(bytes) .order(ByteOrder.LITTLE_ENDIAN);
	 * 
	 * if (bb.remaining() < 43) return;
	 * 
	 * bb.getShort(); // msgType bb.getShort(); // exchange
	 * 
	 * 
	 * 
	 * int securityId = bb.getInt(); //float ltp = bb.getFloat();
	 * 
	 * //bb.position(8); // skip response header
	 * 
	 * float ltp = bb.getFloat(); // 9–12
	 * 
	 * short ltq = bb.getShort(); // 13–14
	 * 
	 * int ltt = bb.getInt(); // 15–18
	 * 
	 * float atp = bb.getFloat(); // 19–22
	 * 
	 * int volume = bb.getInt(); // 23–26
	 * 
	 * int sellQty = bb.getInt(); // 27–30
	 * 
	 * int buyQty = bb.getInt(); // 31–34
	 * 
	 * int oi = bb.getInt(); // 35–38 ✅ current OI
	 * 
	 * int highestOi = bb.getInt(); // 39–42 ✅ day high OI
	 * 
	 * // ✅ Lookup optionType from store String optionType =
	 * store.optionTypeOf(securityId);
	 * 
	 * if (optionType == null) { // not subscribed / unknown instrument return; }
	 * 
	 * // 30 second candle Optional<OptionRsi> ofNullable30 =
	 * Optional.ofNullable(rsiService.onLtp( "NIFTY", securityId, optionType, ltp,
	 * oi, highestOi, atp, 30 ));
	 * 
	 * // 1 minute candle Optional<OptionRsi> ofNullable60 =
	 * Optional.ofNullable(rsiService.onLtp( "NIFTY", securityId, optionType, ltp,
	 * oi, highestOi, atp, 60 )); }
	 */

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
