package com.example.demo.controller;

import com.example.demo.dto.AlpacaAccountInfo;
import com.example.demo.dto.BarDTO;
import com.example.demo.dto.ChartDataDTO;
import com.example.demo.service.AlpacaMarketDataService;
import com.example.demo.service.AlpacaTradeService;
import com.example.demo.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api")
public class AlpacaController {

    private final MarketDataService marketDataService;

    @Autowired(required = false)
    private AlpacaTradeService alpacaTradeService;

    @Autowired(required = false)
    private AlpacaMarketDataService alpacaMarketDataService;

    public AlpacaController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/alpaca/account")
    public ResponseEntity<?> accountInfo() {
        if (alpacaTradeService == null) {
            return ResponseEntity.status(503).body("Alpaca not configured");
        }
        AlpacaAccountInfo info = alpacaTradeService.getAccountInfo();
        return ResponseEntity.ok(info);
    }

    @GetMapping("/chart/{symbol}")
    public ResponseEntity<ChartDataDTO> chartData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "30") int days) {

        String upper = symbol.toUpperCase();
        double rawPrice = marketDataService.getPrice(upper).orElse(100.0);
        BigDecimal currentPrice = BigDecimal.valueOf(rawPrice).setScale(4, RoundingMode.HALF_UP);

        List<BarDTO> bars;
        if (alpacaMarketDataService != null) {
            bars = alpacaMarketDataService.getDailyBars(upper, days);
        } else {
            bars = generateSimulatedBars(currentPrice, days);
        }

        BigDecimal priceChange = BigDecimal.ZERO;
        double priceChangePct = 0.0;
        if (!bars.isEmpty()) {
            BigDecimal firstClose = bars.get(0).getClose();
            BigDecimal lastClose = bars.get(bars.size() - 1).getClose();
            if (firstClose != null && firstClose.compareTo(BigDecimal.ZERO) != 0) {
                priceChange = lastClose.subtract(firstClose).setScale(4, RoundingMode.HALF_UP);
                priceChangePct = Math.round(
                        priceChange.divide(firstClose, 6, RoundingMode.HALF_UP)
                                   .multiply(BigDecimal.valueOf(100))
                                   .doubleValue() * 100.0) / 100.0;
            }
        }

        return ResponseEntity.ok(new ChartDataDTO(upper, bars, currentPrice, priceChange, priceChangePct));
    }

    private List<BarDTO> generateSimulatedBars(BigDecimal anchorPrice, int days) {
        List<BarDTO> bars = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double price = anchorPrice.doubleValue();
        LocalDate today = LocalDate.now();

        // Walk backwards, then reverse so oldest is first
        double[] closes = new double[days];
        closes[days - 1] = price;
        for (int i = days - 2; i >= 0; i--) {
            double change = rng.nextDouble(-0.02, 0.02);
            closes[i] = Math.max(1.0, closes[i + 1] * (1.0 - change));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        for (int i = 0; i < days; i++) {
            LocalDate date = today.minusDays(days - 1 - i);
            double c = Math.round(closes[i] * 100.0) / 100.0;
            double spread = c * rng.nextDouble(0.001, 0.015);
            double o = Math.round((c + rng.nextDouble(-spread, spread)) * 100.0) / 100.0;
            double h = Math.round(Math.max(o, c) * (1 + rng.nextDouble(0, 0.005)) * 100.0) / 100.0;
            double l = Math.round(Math.min(o, c) * (1 - rng.nextDouble(0, 0.005)) * 100.0) / 100.0;
            long vol = rng.nextLong(500_000, 5_000_000);
            bars.add(new BarDTO(
                    date.atStartOfDay().format(fmt),
                    BigDecimal.valueOf(o).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(h).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(l).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(c).setScale(4, RoundingMode.HALF_UP),
                    vol));
        }
        return bars;
    }
}
