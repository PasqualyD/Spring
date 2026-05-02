package com.example.demo.service;

import com.example.demo.model.PriceAlert;
import com.example.demo.model.User;
import com.example.demo.repository.PriceAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PriceAlertService {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertService.class);

    private final PriceAlertRepository priceAlertRepository;
    private final MarketDataService marketDataService;

    public PriceAlertService(PriceAlertRepository priceAlertRepository,
                             MarketDataService marketDataService) {
        this.priceAlertRepository = priceAlertRepository;
        this.marketDataService = marketDataService;
    }

    public PriceAlert createAlert(User user, String symbol, BigDecimal targetPrice, String direction) {
        String upper = symbol.toUpperCase();
        String upperDir = direction.toUpperCase();

        if (marketDataService.getPrice(upper).isEmpty()) {
            throw new IllegalArgumentException("Unknown symbol: " + upper);
        }
        if (!"ABOVE".equals(upperDir) && !"BELOW".equals(upperDir)) {
            throw new IllegalArgumentException("Direction must be ABOVE or BELOW");
        }
        long activeCount = priceAlertRepository.findByUserAndTriggeredFalse(user).size();
        if (activeCount >= 10) {
            throw new IllegalStateException("Maximum of 10 active alerts reached");
        }

        PriceAlert alert = new PriceAlert();
        alert.setUser(user);
        alert.setSymbol(upper);
        alert.setTargetPrice(targetPrice);
        alert.setDirection(upperDir);
        return priceAlertRepository.save(alert);
    }

    public void deleteAlert(Long alertId, User user) {
        priceAlertRepository.findById(alertId).ifPresent(alert -> {
            if (alert.getUser().getId().equals(user.getId())) {
                priceAlertRepository.delete(alert);
            }
        });
    }

    public List<PriceAlert> getActiveAlerts(User user) {
        return priceAlertRepository.findByUserAndTriggeredFalse(user);
    }

    public List<PriceAlert> getAllAlerts(User user) {
        return priceAlertRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public void checkAndTriggerAlerts(String symbol, BigDecimal currentPrice) {
        List<PriceAlert> alerts = priceAlertRepository.findBySymbolAndTriggeredFalse(symbol);
        for (PriceAlert alert : alerts) {
            boolean fire = "ABOVE".equals(alert.getDirection())
                    ? currentPrice.compareTo(alert.getTargetPrice()) >= 0
                    : currentPrice.compareTo(alert.getTargetPrice()) <= 0;
            if (fire) {
                alert.setTriggered(true);
                alert.setTriggeredAt(LocalDateTime.now());
                priceAlertRepository.save(alert);
                log.info("Alert triggered: {} {} {} at {}",
                        alert.getSymbol(), alert.getDirection(),
                        alert.getTargetPrice(), currentPrice);
            }
        }
    }
}
