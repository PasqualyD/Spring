package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "market.data.mode", havingValue = "live")
public class PriceRefreshService {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshService.class);

    private final AlpacaMarketDataService alpacaMarketDataService;
    private final MarketDataService marketDataService;
    private final PriceAlertService priceAlertService;

    public PriceRefreshService(AlpacaMarketDataService alpacaMarketDataService,
                               MarketDataService marketDataService,
                               PriceAlertService priceAlertService) {
        this.alpacaMarketDataService = alpacaMarketDataService;
        this.marketDataService = marketDataService;
        this.priceAlertService = priceAlertService;
    }

    @Scheduled(fixedRateString = "${alpaca.refresh.rate:15000}")
    public void refreshPrices() {
        List<String> symbols = List.copyOf(marketDataService.getAllSymbols());
        if (symbols.isEmpty()) return;

        Map<String, BigDecimal> prices = alpacaMarketDataService.getLatestPrices(symbols);
        int updated = 0;
        for (Map.Entry<String, BigDecimal> entry : prices.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal price = entry.getValue();
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                marketDataService.updateMarketPrice(symbol, price.doubleValue());
                priceAlertService.checkAndTriggerAlerts(symbol, price);
                updated++;
            }
        }
        log.debug("Price refresh tick: {}/{} symbols updated from Alpaca", updated, symbols.size());
    }
}
