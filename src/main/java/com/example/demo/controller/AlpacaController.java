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
import java.time.DayOfWeek;
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
            bars = days <= 5
                    ? alpacaMarketDataService.getIntradayBars(upper, days)
                    : alpacaMarketDataService.getDailyBars(upper, days);
        } else {
            bars = days <= 5
                    ? generateSimulatedIntradayBars(currentPrice, days)
                    : generateSimulatedBars(currentPrice, days);
        }

        BigDecimal openPrice = bars.isEmpty() ? currentPrice : bars.get(0).getOpen();
        BigDecimal lastClose = bars.isEmpty() ? currentPrice : bars.get(bars.size() - 1).getClose();

        BigDecimal priceChange = BigDecimal.ZERO;
        double priceChangePct = 0.0;
        if (openPrice != null && openPrice.compareTo(BigDecimal.ZERO) != 0) {
            priceChange = lastClose.subtract(openPrice).setScale(4, RoundingMode.HALF_UP);
            priceChangePct = Math.round(
                    priceChange.divide(openPrice, 6, RoundingMode.HALF_UP)
                               .multiply(BigDecimal.valueOf(100))
                               .doubleValue() * 100.0) / 100.0;
        }

        BigDecimal high52 = bars.stream()
                .map(BarDTO::getHigh).filter(v -> v != null)
                .max(BigDecimal::compareTo).orElse(currentPrice);
        BigDecimal low52 = bars.stream()
                .map(BarDTO::getLow).filter(v -> v != null)
                .min(BigDecimal::compareTo).orElse(currentPrice);

        return ResponseEntity.ok(new ChartDataDTO(
                upper, bars, currentPrice, openPrice,
                priceChange, priceChangePct, high52, low52));
    }

    private List<BarDTO> generateSimulatedBars(BigDecimal anchorPrice, int days) {
        List<BarDTO> bars = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double price = anchorPrice.doubleValue();
        LocalDate today = LocalDate.now();

        // Collect trading days (skip weekends)
        List<LocalDate> tradingDays = new ArrayList<>();
        LocalDate cursor = today;
        while (tradingDays.size() < days) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                tradingDays.add(0, cursor);
            }
            cursor = cursor.minusDays(1);
        }

        double[] closes = new double[tradingDays.size()];
        closes[closes.length - 1] = price;
        for (int i = closes.length - 2; i >= 0; i--) {
            double change = rng.nextDouble(-0.02, 0.02);
            closes[i] = Math.max(1.0, closes[i + 1] * (1.0 - change));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        for (int i = 0; i < tradingDays.size(); i++) {
            double c = Math.round(closes[i] * 100.0) / 100.0;
            double spread = c * rng.nextDouble(0.001, 0.015);
            double o = Math.round((c + rng.nextDouble(-spread, spread)) * 100.0) / 100.0;
            double h = Math.round(Math.max(o, c) * (1 + rng.nextDouble(0, 0.01)) * 100.0) / 100.0;
            double l = Math.round(Math.min(o, c) * (1 - rng.nextDouble(0, 0.01)) * 100.0) / 100.0;
            long vol = rng.nextLong(1_000_000, 50_000_000);
            bars.add(new BarDTO(
                    tradingDays.get(i).atStartOfDay().format(fmt),
                    BigDecimal.valueOf(o).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(h).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(l).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(c).setScale(4, RoundingMode.HALF_UP),
                    vol));
        }
        return bars;
    }

    private List<BarDTO> generateSimulatedIntradayBars(BigDecimal anchorPrice, int days) {
        List<BarDTO> bars = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double price = anchorPrice.doubleValue();
        LocalDate today = LocalDate.now();

        List<LocalDate> tradingDays = new ArrayList<>();
        LocalDate cursor = today;
        while (tradingDays.size() < days) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                tradingDays.add(0, cursor);
            }
            cursor = cursor.minusDays(1);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        double dayClose = price;
        for (LocalDate day : tradingDays) {
            // Walk 7 hourly bars per day (9:30–16:00 roughly)
            double[] hourCloses = new double[7];
            hourCloses[6] = dayClose;
            for (int h = 5; h >= 0; h--) {
                double change = rng.nextDouble(-0.005, 0.005);
                hourCloses[h] = Math.max(1.0, hourCloses[h + 1] * (1.0 - change));
            }
            dayClose = hourCloses[0] * (1 + rng.nextDouble(-0.015, 0.015));
            for (int h = 0; h < 7; h++) {
                double c = Math.round(hourCloses[h] * 100.0) / 100.0;
                double spread = c * rng.nextDouble(0.0005, 0.005);
                double o = Math.round((c + rng.nextDouble(-spread, spread)) * 100.0) / 100.0;
                double hi = Math.round(Math.max(o, c) * (1 + rng.nextDouble(0, 0.005)) * 100.0) / 100.0;
                double lo = Math.round(Math.min(o, c) * (1 - rng.nextDouble(0, 0.005)) * 100.0) / 100.0;
                long vol = rng.nextLong(100_000, 5_000_000);
                String ts = day.atTime(9 + h, 30).format(fmt);
                bars.add(new BarDTO(
                        ts,
                        BigDecimal.valueOf(o).setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(hi).setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(lo).setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(c).setScale(4, RoundingMode.HALF_UP),
                        vol));
            }
        }
        return bars;
    }
}
