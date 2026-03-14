package com.example.dhan_rsi_series.service;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

import com.example.dhan_rsi_series.utils.DhanLiveDataHandler;

import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/*@Service
public class DhanReactiveWebSocketService {

    private final WebSocketClient client = new ReactorNettyWebSocketClient();
    private final DhanLiveDataHandler handler;
    private final DhanAuthService authService;

    public DhanReactiveWebSocketService(DhanLiveDataHandler handler, DhanAuthService authService) {
        this.handler = handler;
        this.authService = authService;
    }

    @PostConstruct
    public void connect() {

        String clientId = "1106289803";

        authService.getAccessToken()
            .flatMapMany(token -> {
                String wsUrl = String.format(
                    "wss://api-feed.dhan.co/?version=2&token=%s&clientId=%s&authType=2",
                    token,
                    clientId
                );

                return client.execute(
                        URI.create(wsUrl),
                        session -> session.receive()
                            .map(WebSocketMessage::getPayloadAsText) // <-- convert to String
                            .doOnNext(msg -> System.out.println("Received: " + msg))
                            .then() // Important: returns Mono<Void>
                );
            })
            .doOnError(err -> System.err.println("WebSocket error: " + err.getMessage()))
            .subscribe();
    }


}*/

/*@Service
public class DhanReactiveWebSocketService {

    private final ReactorNettyWebSocketClient client;
    private final DhanLiveDataHandler handler;
    private final DhanAuthService authService;

    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(5);

    public DhanReactiveWebSocketService(
            ReactorNettyWebSocketClient client,
            DhanLiveDataHandler handler,
            DhanAuthService authService) {

        this.client = client;
        this.handler = handler;
        this.authService = authService;
    }

    @PostConstruct
    public void connect() {
        startConnection()
            .retryWhen(
                Retry.fixedDelay(Long.MAX_VALUE, RECONNECT_DELAY)
                    .doBeforeRetry(rs ->
                        System.err.println(
                            "🔁 Reconnecting... attempt "
                            + rs.totalRetriesInARow()
                        )
                    )
            )
            .subscribe();
    }*/

    /**
     * Single connection attempt
     */
   /* private Mono<Void> startConnection() {

        return authService.getAccessToken()
            .flatMap(token -> {

                String wsUrl =
                    "wss://api-feed.dhan.co/"
                    + "?version=2"
                    + "&token=" + token
                    + "&clientId=1106289803"
                    + "&authType=2";

                System.out.println("🔌 Connecting to Dhan WebSocket");

                return client.execute(
                    URI.create(wsUrl),
                    handler
                );
            })
            .doOnError(e ->
                System.err.println("❌ WebSocket error: " + e.getMessage())
            )
            .doOnSuccess(v ->
                System.out.println("🔌 WebSocket connection closed")
            );
    }
}*/

/*@Service
public class DhanReactiveWebSocketService {

    private final ReactorNettyWebSocketClient client;
    private final DhanLiveDataHandler handler;
    private final DhanAuthService authService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public DhanReactiveWebSocketService(
            ReactorNettyWebSocketClient client,
            DhanLiveDataHandler handler,
            DhanAuthService authService) {

        this.client = client;
        this.handler = handler;
        this.authService = authService;
    }

    @PostConstruct
    public void start() {

        if (!running.compareAndSet(false, true)) {
            return;
        }

        connectLoop()
            .subscribe();
    }

    private Mono<Void> connectLoop() {

        return authService.getAccessToken()
            .flatMap(token -> {

                String wsUrl =
                    "wss://api-feed.dhan.co/"
                    + "?version=2"
                    + "&token=" + token
                    + "&clientId=1106289803"
                    + "&authType=2";

                return client.execute(
                    URI.create(wsUrl),
                    handler
                );
            })
            .doOnSubscribe(s ->
                System.out.println("🟢 WebSocket connecting...")
            )
            .doOnError(e ->
                System.err.println("❌ WebSocket error: " + e.getMessage())
            )
            .doOnTerminate(() ->
                System.out.println("🔌 WebSocket disconnected")
            )
            .retryWhen(
                Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(5))
                     .doBeforeRetry(r ->
                         System.out.println("🔁 Reconnecting... attempt " + r.totalRetries())
                     )
            );
    }
}*/

@Service
public class DhanReactiveWebSocketService {

    private final ReactorNettyWebSocketClient client;
    private final DhanLiveDataHandler handler;
    private final DhanAuthService authService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public DhanReactiveWebSocketService(
            ReactorNettyWebSocketClient client,
            DhanLiveDataHandler handler,
            DhanAuthService authService) {

        this.client = client;
        this.handler = handler;
        this.authService = authService;
    }

    @PostConstruct
    public void start() {

        if (!running.compareAndSet(false, true)) return;

        connect()
            .retryWhen(reconnectPolicy())
            .subscribe();
    }

    private Mono<Void> connect() {

        return authService.getAccessToken()
            .flatMap(token -> {

                String wsUrl =
                    "wss://api-feed.dhan.co/"
                    + "?version=2"
                    + "&token=" + token
                    + "&clientId=1106289803"
                    + "&authType=2";

                System.out.println("🟢 WebSocket connecting...");
                return client.execute(URI.create(wsUrl), handler);
            })
            .doOnError(e ->
                System.err.println("❌ WebSocket error: " + e.getMessage())
            )
            .doOnTerminate(() ->
                System.out.println("🔌 WebSocket disconnected")
            );
    }

    private Retry reconnectPolicy() {

        return Retry
            .backoff(Long.MAX_VALUE, Duration.ofSeconds(10))
            .maxBackoff(Duration.ofMinutes(2))
            .jitter(0.3)
            .filter(this::isRetryable)
            .doBeforeRetry(signal ->
                System.out.println(
                    "🔁 Reconnecting... attempt " +
                    (signal.totalRetries() + 1)
                )
            );
    }

    private boolean isRetryable(Throwable t) {

        // ❌ Handshake rejected (429 / throttling)
        if (t instanceof WebSocketClientHandshakeException) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("429")) {
                System.err.println("🚫 Rate limited by Dhan. Cooling down...");
                return false; // ❌ STOP retrying immediately
            }
        }

        // ❌ Closed before handshake
        if (t.getMessage() != null &&
            t.getMessage().contains("prematurely closed")) {
            return true; // retry, but with backoff
        }

        return true;
    }
}