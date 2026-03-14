package com.example.dhan_rsi_series.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

import io.netty.channel.ChannelOption;
import io.netty.resolver.DefaultAddressResolverGroup;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

/*@Configuration
public class DhanWebSocketConfig {

	@Bean
    public ReactorNettyWebSocketClient dhanWebSocketClient() {

        HttpClient httpClient = HttpClient.create()
            .protocol(HttpProtocol.HTTP11) // 🔴 REQUIRED
            .headers(h -> {
                h.add("Origin", "https://dhan.co");
                h.add("User-Agent", "Mozilla/5.0");
            });


        return new ReactorNettyWebSocketClient(httpClient);
    }

}
*/

@Configuration
public class DhanWebSocketConfig {

    @Bean
    public ReactorNettyWebSocketClient dhanWebSocketClient() {

        HttpClient httpClient = HttpClient.create()
            .protocol(HttpProtocol.HTTP11)
            .resolver(DefaultAddressResolverGroup.INSTANCE)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .headers(h -> {
                h.add("Origin", "https://dhan.co");
                h.add("User-Agent", "Mozilla/5.0");
            })
            .keepAlive(true)
            .compress(true);

        return new ReactorNettyWebSocketClient(httpClient);
    }
}

