package com.example.demo.service;

import com.example.demo.dto.AlpacaAccountInfo;
import com.example.demo.dto.AlpacaOrderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "market.data.mode", havingValue = "live")
public class AlpacaTradeService {

    private static final Logger log = LoggerFactory.getLogger(AlpacaTradeService.class);
    private static final long CLOCK_CACHE_TTL_MS = 60_000;

    private final WebClient paperClient;

    private final AtomicBoolean cachedMarketOpen = new AtomicBoolean(false);
    private volatile long lastClockFetch = 0;

    public AlpacaTradeService(@Qualifier("alpacaPaperClient") WebClient paperClient) {
        this.paperClient = paperClient;
    }

    @SuppressWarnings("unchecked")
    public AlpacaOrderResult submitMarketOrder(String symbol, String side, BigDecimal quantity) {
        AlpacaOrderResult result = new AlpacaOrderResult();
        result.setSymbol(symbol);
        result.setSide(side);
        try {
            Map<String, String> body = Map.of(
                    "symbol", symbol,
                    "qty", quantity.toPlainString(),
                    "side", side.toLowerCase(),
                    "type", "market",
                    "time_in_force", "day"
            );

            Map<String, Object> response = paperClient.post()
                    .uri("/v2/orders")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response == null) {
                result.setErrorMessage("No response from Alpaca");
                return result;
            }

            result.setAlpacaOrderId((String) response.get("id"));
            result.setStatus((String) response.get("status"));
            result.setSubmittedAt((String) response.get("submitted_at"));

            Object filledQty = response.get("filled_qty");
            if (filledQty instanceof String s && !s.isEmpty()) {
                result.setFilledQty(new BigDecimal(s));
            }
            Object filledAvg = response.get("filled_avg_price");
            if (filledAvg instanceof String s && !s.isEmpty()) {
                result.setFilledAvgPrice(new BigDecimal(s).setScale(4, RoundingMode.HALF_UP));
            }

        } catch (WebClientResponseException e) {
            log.warn("Alpaca order rejected {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            result.setErrorMessage("Alpaca rejected order: " + e.getStatusCode());
        } catch (Exception e) {
            log.warn("submitMarketOrder failed: {}", e.getMessage());
            result.setErrorMessage("Alpaca unavailable: " + e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public AlpacaOrderResult getOrderStatus(String alpacaOrderId) {
        AlpacaOrderResult result = new AlpacaOrderResult();
        result.setAlpacaOrderId(alpacaOrderId);
        try {
            Map<String, Object> response = paperClient.get()
                    .uri("/v2/orders/{id}", alpacaOrderId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response == null) { result.setErrorMessage("No response"); return result; }

            result.setStatus((String) response.get("status"));
            result.setSymbol((String) response.get("symbol"));
            Object filledAvg = response.get("filled_avg_price");
            if (filledAvg instanceof String s && !s.isEmpty()) {
                result.setFilledAvgPrice(new BigDecimal(s).setScale(4, RoundingMode.HALF_UP));
            }
        } catch (Exception e) {
            log.warn("getOrderStatus({}) failed: {}", alpacaOrderId, e.getMessage());
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public AlpacaAccountInfo getAccountInfo() {
        AlpacaAccountInfo info = new AlpacaAccountInfo();
        try {
            Map<String, Object> response = paperClient.get()
                    .uri("/v2/account")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response == null) return info;

            info.setBuyingPower(parseBd(response.get("buying_power")));
            info.setPortfolioValue(parseBd(response.get("portfolio_value")));
            info.setEquity(parseBd(response.get("equity")));
            info.setCash(parseBd(response.get("cash")));
            info.setAccountStatus((String) response.get("status"));
        } catch (Exception e) {
            log.warn("getAccountInfo failed: {}", e.getMessage());
        }
        return info;
    }

    @SuppressWarnings("unchecked")
    public boolean isMarketOpen() {
        long now = System.currentTimeMillis();
        if (now - lastClockFetch < CLOCK_CACHE_TTL_MS) {
            return cachedMarketOpen.get();
        }
        try {
            Map<String, Object> response = paperClient.get()
                    .uri("/v2/clock")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            boolean open = response != null && Boolean.TRUE.equals(response.get("is_open"));
            cachedMarketOpen.set(open);
            lastClockFetch = now;
            return open;
        } catch (Exception e) {
            log.warn("isMarketOpen check failed: {}", e.getMessage());
            return cachedMarketOpen.get();
        }
    }

    private BigDecimal parseBd(Object val) {
        if (val instanceof String s && !s.isEmpty()) {
            try { return new BigDecimal(s).setScale(2, RoundingMode.HALF_UP); } catch (Exception ignored) {}
        }
        if (val instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
