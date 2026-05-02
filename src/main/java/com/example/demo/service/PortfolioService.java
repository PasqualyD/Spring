package com.example.demo.service;

import com.example.demo.dto.TradingMetrics;
import com.example.demo.model.Portfolio;
import com.example.demo.model.Position;
import com.example.demo.model.TradeRecord;
import com.example.demo.model.User;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.PositionRepository;
import com.example.demo.repository.TradeRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TradeRecordRepository tradeRecordRepository;
    private final MarketDataService marketDataService;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            PositionRepository positionRepository,
                            TradeRecordRepository tradeRecordRepository,
                            MarketDataService marketDataService) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.tradeRecordRepository = tradeRecordRepository;
        this.marketDataService = marketDataService;
    }

    public Portfolio getPortfolio(User user) {
        return portfolioRepository.findByUser(user).orElseGet(() -> {
            Portfolio p = new Portfolio();
            p.setUser(user);
            return portfolioRepository.save(p);
        });
    }

    public TradeResult executeTrade(User user, String symbol, String side, BigDecimal quantity) {
        Portfolio portfolio = getPortfolio(user);
        String upperSymbol = symbol.toUpperCase();
        String upperSide = side.toUpperCase();

        Optional<Double> priceOpt = marketDataService.getPrice(upperSymbol);
        if (priceOpt.isEmpty()) {
            TradeRecord record = buildRecord(portfolio, upperSymbol, upperSide, quantity,
                    BigDecimal.ZERO, "REJECTED", "Symbol not found: " + upperSymbol);
            tradeRecordRepository.save(record);
            return new TradeResult(false, "Symbol not found: " + upperSymbol, record);
        }

        BigDecimal price = BigDecimal.valueOf(priceOpt.get()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalValue = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);

        if ("BUY".equals(upperSide)) {
            if (portfolio.getCashBalance().compareTo(totalValue) < 0) {
                TradeRecord record = buildRecord(portfolio, upperSymbol, upperSide, quantity,
                        price, "REJECTED", "Insufficient funds");
                tradeRecordRepository.save(record);
                return new TradeResult(false, "Insufficient funds", record);
            }

            portfolio.setCashBalance(portfolio.getCashBalance().subtract(totalValue));

            Optional<Position> existingOpt = positionRepository.findByPortfolioAndSymbol(portfolio, upperSymbol);
            if (existingOpt.isPresent()) {
                Position pos = existingOpt.get();
                BigDecimal newQty = pos.getQuantity().add(quantity);
                BigDecimal newAvg = pos.getQuantity().multiply(pos.getAverageCostBasis())
                        .add(quantity.multiply(price))
                        .divide(newQty, 4, RoundingMode.HALF_UP);
                pos.setQuantity(newQty);
                pos.setAverageCostBasis(newAvg);
                positionRepository.save(pos);
            } else {
                Position pos = new Position();
                pos.setPortfolio(portfolio);
                pos.setSymbol(upperSymbol);
                pos.setQuantity(quantity);
                pos.setAverageCostBasis(price);
                positionRepository.save(pos);
            }

            portfolioRepository.save(portfolio);

        } else if ("SELL".equals(upperSide)) {
            Optional<Position> existingOpt = positionRepository.findByPortfolioAndSymbol(portfolio, upperSymbol);
            if (existingOpt.isEmpty()) {
                TradeRecord record = buildRecord(portfolio, upperSymbol, upperSide, quantity,
                        price, "REJECTED", "Position not found");
                tradeRecordRepository.save(record);
                return new TradeResult(false, "No open position for " + upperSymbol, record);
            }

            Position pos = existingOpt.get();
            if (pos.getQuantity().compareTo(quantity) < 0) {
                TradeRecord record = buildRecord(portfolio, upperSymbol, upperSide, quantity,
                        price, "REJECTED", "Insufficient shares");
                tradeRecordRepository.save(record);
                return new TradeResult(false, "Insufficient shares", record);
            }

            BigDecimal remaining = pos.getQuantity().subtract(quantity);
            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                positionRepository.delete(pos);
            } else {
                pos.setQuantity(remaining);
                positionRepository.save(pos);
            }

            portfolio.setCashBalance(portfolio.getCashBalance().add(totalValue));
            portfolioRepository.save(portfolio);

        } else {
            TradeRecord record = buildRecord(portfolio, upperSymbol, upperSide, quantity,
                    price, "REJECTED", "Invalid side: " + side);
            tradeRecordRepository.save(record);
            return new TradeResult(false, "Invalid side: " + side, record);
        }

        TradeRecord record = buildRecord(portfolio, upperSymbol, upperSide, quantity, price, "FILLED", null);
        tradeRecordRepository.save(record);
        return new TradeResult(true,
                upperSide + " " + quantity.stripTrailingZeros().toPlainString() + " " + upperSymbol + " @ $" + price.setScale(2, RoundingMode.HALF_UP),
                record);
    }

    public List<PositionWithValue> getPositionsWithValue(User user) {
        Portfolio portfolio = getPortfolio(user);
        List<Position> positions = positionRepository.findByPortfolio(portfolio);
        List<PositionWithValue> result = new ArrayList<>();

        for (Position pos : positions) {
            double rawPrice = marketDataService.getPrice(pos.getSymbol())
                    .orElse(pos.getAverageCostBasis().doubleValue());
            BigDecimal currentPrice = BigDecimal.valueOf(rawPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal marketValue = currentPrice.multiply(pos.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal costBasis = pos.getAverageCostBasis() != null ? pos.getAverageCostBasis() : currentPrice;
            BigDecimal unrealizedPnl = currentPrice.subtract(costBasis)
                    .multiply(pos.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalCost = costBasis.multiply(pos.getQuantity());
            BigDecimal pnlPercent = totalCost.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : unrealizedPnl.divide(totalCost, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            result.add(new PositionWithValue(pos, currentPrice, marketValue, unrealizedPnl, pnlPercent));
        }
        return result;
    }

    public PortfolioSummary getPortfolioSummary(User user) {
        Portfolio portfolio = getPortfolio(user);
        List<Position> positions = positionRepository.findByPortfolio(portfolio);

        BigDecimal totalPositionValue = BigDecimal.ZERO;
        for (Position pos : positions) {
            double rawPrice = marketDataService.getPrice(pos.getSymbol()).orElse(0.0);
            totalPositionValue = totalPositionValue.add(
                    BigDecimal.valueOf(rawPrice).multiply(pos.getQuantity()));
        }
        totalPositionValue = totalPositionValue.setScale(2, RoundingMode.HALF_UP);

        BigDecimal cashBalance = portfolio.getCashBalance();
        BigDecimal totalPortfolioValue = cashBalance.add(totalPositionValue);
        BigDecimal deposited = portfolio.getTotalDeposited();

        BigDecimal totalReturnPct = deposited.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : totalPortfolioValue.subtract(deposited)
                        .divide(deposited, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        return new PortfolioSummary(cashBalance, totalPositionValue, totalPortfolioValue, totalReturnPct, positions.size());
    }

    public List<TradeRecord> getRecentTrades(User user, int limit) {
        Portfolio portfolio = getPortfolio(user);
        return tradeRecordRepository.findByPortfolioOrderByExecutedAtDesc(portfolio, PageRequest.of(0, limit));
    }

    public TradingMetrics getMetrics(User user) {
        Portfolio portfolio = getPortfolio(user);
        List<TradeRecord> allTrades = tradeRecordRepository.findByPortfolioOrderByExecutedAtDesc(portfolio);
        List<TradeRecord> filled = allTrades.stream()
                .filter(t -> "FILLED".equals(t.getStatus()))
                .toList();

        int totalTrades = filled.size();
        int totalBuys = (int) filled.stream().filter(t -> "BUY".equals(t.getSide())).count();
        int totalSells = (int) filled.stream().filter(t -> "SELL".equals(t.getSide())).count();

        BigDecimal totalVolumeTraded = filled.stream()
                .map(TradeRecord::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal averageTradeSize = totalTrades == 0 ? BigDecimal.ZERO
                : totalVolumeTraded.divide(BigDecimal.valueOf(totalTrades), 2, RoundingMode.HALF_UP);

        // Most traded symbol
        Map<String, Long> symbolCount = new HashMap<>();
        for (TradeRecord t : filled) {
            symbolCount.merge(t.getSymbol(), 1L, Long::sum);
        }
        String mostTradedSymbol = symbolCount.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("—");

        // Win rate: group sells by symbol, match against weighted avg buy price
        Map<String, BigDecimal> totalCostBySymbol = new HashMap<>();
        Map<String, BigDecimal> totalQtyBySymbol = new HashMap<>();
        for (TradeRecord t : filled) {
            if ("BUY".equals(t.getSide())) {
                totalCostBySymbol.merge(t.getSymbol(), t.getTotalValue(), BigDecimal::add);
                totalQtyBySymbol.merge(t.getSymbol(), t.getQuantity(), BigDecimal::add);
            }
        }

        int wins = 0;
        int totalClosed = 0;
        BigDecimal bestTrade = null;
        BigDecimal worstTrade = null;

        for (TradeRecord t : filled) {
            if (!"SELL".equals(t.getSide())) continue;
            BigDecimal totalCost = totalCostBySymbol.get(t.getSymbol());
            BigDecimal totalQty = totalQtyBySymbol.get(t.getSymbol());
            if (totalCost == null || totalQty == null || totalQty.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal avgBuy = totalCost.divide(totalQty, 4, RoundingMode.HALF_UP);
            BigDecimal costBasis = avgBuy.multiply(t.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profit = t.getTotalValue().subtract(costBasis);

            totalClosed++;
            if (profit.compareTo(BigDecimal.ZERO) > 0) wins++;
            if (bestTrade == null || profit.compareTo(bestTrade) > 0) bestTrade = profit;
            if (worstTrade == null || profit.compareTo(worstTrade) < 0) worstTrade = profit;
        }

        double winRate = totalClosed == 0 ? 0.0
                : Math.round((wins * 10000.0) / totalClosed) / 100.0;

        return new TradingMetrics(totalTrades, totalBuys, totalSells, totalVolumeTraded,
                winRate,
                bestTrade != null ? bestTrade : BigDecimal.ZERO,
                worstTrade != null ? worstTrade : BigDecimal.ZERO,
                averageTradeSize, mostTradedSymbol);
    }

    private TradeRecord buildRecord(Portfolio portfolio, String symbol, String side,
                                    BigDecimal quantity, BigDecimal price, String status, String rejectionReason) {
        TradeRecord r = new TradeRecord();
        r.setPortfolio(portfolio);
        r.setSymbol(symbol);
        r.setSide(side);
        r.setQuantity(quantity);
        r.setPriceAtExecution(price);
        r.setTotalValue(quantity.multiply(price).setScale(2, RoundingMode.HALF_UP));
        r.setStatus(status);
        r.setRejectionReason(rejectionReason);
        return r;
    }

    // ── Inner DTOs ──────────────────────────────────────────────────────────

    public static class TradeResult {
        private final boolean success;
        private final String message;
        private final TradeRecord tradeRecord;

        public TradeResult(boolean success, String message, TradeRecord tradeRecord) {
            this.success = success;
            this.message = message;
            this.tradeRecord = tradeRecord;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public TradeRecord getTradeRecord() { return tradeRecord; }
    }

    public static class PositionWithValue {
        private final Position position;
        private final BigDecimal currentPrice;
        private final BigDecimal marketValue;
        private final BigDecimal unrealizedPnl;
        private final BigDecimal pnlPercent;

        public PositionWithValue(Position position, BigDecimal currentPrice,
                                 BigDecimal marketValue, BigDecimal unrealizedPnl, BigDecimal pnlPercent) {
            this.position = position;
            this.currentPrice = currentPrice;
            this.marketValue = marketValue;
            this.unrealizedPnl = unrealizedPnl;
            this.pnlPercent = pnlPercent;
        }

        public Position getPosition() { return position; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public BigDecimal getMarketValue() { return marketValue; }
        public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
        public BigDecimal getPnlPercent() { return pnlPercent; }

        // Convenience pass-throughs so templates can stay flat
        public String getSymbol() { return position.getSymbol(); }
        public BigDecimal getQuantity() { return position.getQuantity(); }
        public BigDecimal getAverageCostBasis() { return position.getAverageCostBasis(); }
    }

    public static class PortfolioSummary {
        private final BigDecimal cashBalance;
        private final BigDecimal totalPositionValue;
        private final BigDecimal totalPortfolioValue;
        private final BigDecimal totalReturnPct;
        private final int positionCount;

        public PortfolioSummary(BigDecimal cashBalance, BigDecimal totalPositionValue,
                                BigDecimal totalPortfolioValue, BigDecimal totalReturnPct, int positionCount) {
            this.cashBalance = cashBalance;
            this.totalPositionValue = totalPositionValue;
            this.totalPortfolioValue = totalPortfolioValue;
            this.totalReturnPct = totalReturnPct;
            this.positionCount = positionCount;
        }

        public BigDecimal getCashBalance() { return cashBalance; }
        public BigDecimal getTotalPositionValue() { return totalPositionValue; }
        public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
        public BigDecimal getTotalReturnPct() { return totalReturnPct; }
        public int getPositionCount() { return positionCount; }
    }
}
