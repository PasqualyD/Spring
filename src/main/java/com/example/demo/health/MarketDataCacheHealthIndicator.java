package com.example.demo.health;

import com.example.demo.service.MarketDataCache;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MarketDataCacheHealthIndicator implements HealthIndicator {

    private final MarketDataCache marketDataCache;

    public MarketDataCacheHealthIndicator(MarketDataCache marketDataCache) {
        this.marketDataCache = marketDataCache;
    }

    @Override
    public Health health() {
        if (marketDataCache.size() > 0) {
            return Health.up().withDetail("marketDataCacheSize", marketDataCache.size()).build();
        }
        return Health.down().withDetail("message", "Market data cache is not initialized").build();
    }
}
