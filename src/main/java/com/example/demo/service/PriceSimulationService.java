package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PriceSimulationService {

    private static final Logger log = LoggerFactory.getLogger(PriceSimulationService.class);

    private final MarketDataService marketDataService;
    private final PriceAlertService priceAlertService;

    public PriceSimulationService(MarketDataService marketDataService,
                                  PriceAlertService priceAlertService) {
        this.marketDataService = marketDataService;
        this.priceAlertService = priceAlertService;
    }

    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void simulatePriceMovements() {
        Set<String> symbols = marketDataService.getAllSymbols();
        if (symbols.isEmpty()) return;

        for (String symbol : symbols) {
            marketDataService.getPrice(symbol).ifPresent(current -> {
                double change = ThreadLocalRandom.current().nextDouble(-0.015, 0.015);
                double raw = current * (1.0 + change);
                double newPrice = Math.round(raw * 100.0) / 100.0;
                if (newPrice > 0) {
                    marketDataService.updateMarketPrice(symbol, newPrice);
                    priceAlertService.checkAndTriggerAlerts(symbol, BigDecimal.valueOf(newPrice));
                }
            });
        }
        log.debug("Price simulation tick: {} symbols updated", symbols.size());
    }
}
