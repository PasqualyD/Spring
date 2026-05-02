package com.example.demo.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class AlpacaConfig {

    @Value("${alpaca.api.key:}")
    private String apiKey;

    @Value("${alpaca.api.secret:}")
    private String apiSecret;

    @Value("${alpaca.paper.base-url}")
    private String paperBaseUrl;

    @Value("${alpaca.data.base-url}")
    private String dataBaseUrl;

    private HttpClient httpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5));
    }

    @Bean("alpacaPaperClient")
    public WebClient alpacaPaperClient() {
        return WebClient.builder()
                .baseUrl(paperBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient()))
                .defaultHeader("APCA-API-KEY-ID", apiKey)
                .defaultHeader("APCA-API-SECRET-KEY", apiSecret)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean("alpacaDataClient")
    public WebClient alpacaDataClient() {
        return WebClient.builder()
                .baseUrl(dataBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient()))
                .defaultHeader("APCA-API-KEY-ID", apiKey)
                .defaultHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();
    }
}
