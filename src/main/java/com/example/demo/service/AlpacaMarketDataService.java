package com.example.demo.service;

import com.example.demo.dto.BarDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "market.data.mode", havingValue = "live")
public class AlpacaMarketDataService {

    private static final Logger log = LoggerFactory.getLogger(AlpacaMarketDataService.class);

    private final WebClient dataClient;

    public AlpacaMarketDataService(@Qualifier("alpacaDataClient") WebClient dataClient) {
        this.dataClient = dataClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, BigDecimal> getLatestPrices(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return Collections.emptyMap();
        try {
            String symbolsCsv = String.join(",", symbols);
            Map<String, Object> response = dataClient.get()
                    .uri(u -> u.path("/v2/stocks/snapshots")
                               .queryParam("symbols", symbolsCsv)
                               .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .onErrorReturn(Collections.emptyMap())
                    .block();

            if (response == null || response.isEmpty()) return Collections.emptyMap();

            Map<String, BigDecimal> prices = new HashMap<>();
            for (Map.Entry<String, Object> entry : response.entrySet()) {
                try {
                    Map<String, Object> snap = (Map<String, Object>) entry.getValue();
                    Map<String, Object> latestTrade = (Map<String, Object>) snap.get("latestTrade");
                    if (latestTrade == null) continue;
                    Object p = latestTrade.get("p");
                    if (p instanceof Number num) {
                        prices.put(entry.getKey(),
                                BigDecimal.valueOf(num.doubleValue()).setScale(4, RoundingMode.HALF_UP));
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse snapshot for {}: {}", entry.getKey(), e.getMessage());
                }
            }
            return prices;
        } catch (Exception e) {
            log.warn("getLatestPrices failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    public BigDecimal getLatestPrice(String symbol) {
        try {
            Map<String, Object> response = dataClient.get()
                    .uri("/v2/stocks/{symbol}/snapshot", symbol)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .onErrorReturn(Collections.emptyMap())
                    .block();

            if (response == null || response.isEmpty()) return null;
            Map<String, Object> latestTrade = (Map<String, Object>) response.get("latestTrade");
            if (latestTrade == null) return null;
            Object p = latestTrade.get("p");
            if (p instanceof Number num) {
                return BigDecimal.valueOf(num.doubleValue()).setScale(4, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("getLatestPrice({}) failed: {}", symbol, e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<BarDTO> getIntradayBars(String symbol, int days) {
        try {
            int limit = days * 7;
            Map<String, Object> response = dataClient.get()
                    .uri(u -> u.path("/v2/stocks/{symbol}/bars")
                               .queryParam("timeframe", "1Hour")
                               .queryParam("limit", limit)
                               .build(symbol))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .onErrorReturn(Collections.emptyMap())
                    .block();

            if (response == null || response.isEmpty()) return Collections.emptyList();
            List<Map<String, Object>> bars = (List<Map<String, Object>>) response.get("bars");
            if (bars == null) return Collections.emptyList();

            List<BarDTO> result = new ArrayList<>();
            for (Map<String, Object> bar : bars) {
                try {
                    String t = (String) bar.get("t");
                    BigDecimal o = toBd(bar.get("o"));
                    BigDecimal h = toBd(bar.get("h"));
                    BigDecimal l = toBd(bar.get("l"));
                    BigDecimal c = toBd(bar.get("c"));
                    long v = bar.get("v") instanceof Number n ? n.longValue() : 0L;
                    result.add(new BarDTO(t, o, h, l, c, v));
                } catch (Exception e) {
                    log.warn("Failed to parse intraday bar for {}: {}", symbol, e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("getIntradayBars({}) failed: {}", symbol, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<BarDTO> getDailyBars(String symbol, int days) {
        try {
            Map<String, Object> response = dataClient.get()
                    .uri(u -> u.path("/v2/stocks/{symbol}/bars")
                               .queryParam("timeframe", "1Day")
                               .queryParam("limit", days)
                               .build(symbol))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .onErrorReturn(Collections.emptyMap())
                    .block();

            if (response == null || response.isEmpty()) return Collections.emptyList();

            List<Map<String, Object>> bars = (List<Map<String, Object>>) response.get("bars");
            if (bars == null) return Collections.emptyList();

            List<BarDTO> result = new ArrayList<>();
            for (Map<String, Object> bar : bars) {
                try {
                    String t = (String) bar.get("t");
                    BigDecimal o = toBd(bar.get("o"));
                    BigDecimal h = toBd(bar.get("h"));
                    BigDecimal l = toBd(bar.get("l"));
                    BigDecimal c = toBd(bar.get("c"));
                    long v = bar.get("v") instanceof Number n ? n.longValue() : 0L;
                    result.add(new BarDTO(t, o, h, l, c, v));
                } catch (Exception e) {
                    log.warn("Failed to parse bar for {}: {}", symbol, e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("getDailyBars({}) failed: {}", symbol, e.getMessage());
            return Collections.emptyList();
        }
    }

    private BigDecimal toBd(Object val) {
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).setScale(4, RoundingMode.HALF_UP);
        return BigDecimal.ZERO;
    }
}
