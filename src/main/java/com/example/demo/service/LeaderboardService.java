package com.example.demo.service;

import com.example.demo.dto.LeaderboardEntry;
import com.example.demo.model.Portfolio;
import com.example.demo.model.Position;
import com.example.demo.model.TradeRecord;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.PositionRepository;
import com.example.demo.repository.TradeRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class LeaderboardService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TradeRecordRepository tradeRecordRepository;
    private final MarketDataService marketDataService;

    public LeaderboardService(PortfolioRepository portfolioRepository,
                              PositionRepository positionRepository,
                              TradeRecordRepository tradeRecordRepository,
                              MarketDataService marketDataService) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.tradeRecordRepository = tradeRecordRepository;
        this.marketDataService = marketDataService;
    }

    public List<LeaderboardEntry> getLeaderboard() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (Portfolio portfolio : portfolios) {
            List<Position> positions = positionRepository.findByPortfolio(portfolio);
            BigDecimal positionsValue = BigDecimal.ZERO;
            for (Position pos : positions) {
                double rawPrice = marketDataService.getPrice(pos.getSymbol()).orElse(0.0);
                positionsValue = positionsValue.add(
                        BigDecimal.valueOf(rawPrice).multiply(pos.getQuantity()));
            }
            BigDecimal totalValue = portfolio.getCashBalance().add(positionsValue)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal deposited = portfolio.getTotalDeposited();
            double returnPct = deposited.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                    : totalValue.subtract(deposited)
                            .divide(deposited, 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
            returnPct = Math.round(returnPct * 100.0) / 100.0;

            List<TradeRecord> trades = tradeRecordRepository.findByPortfolioOrderByExecutedAtDesc(portfolio);
            int tradeCount = (int) trades.stream().filter(t -> "FILLED".equals(t.getStatus())).count();

            Map<String, Long> symbolCount = new HashMap<>();
            for (TradeRecord t : trades) {
                if ("FILLED".equals(t.getStatus())) {
                    symbolCount.merge(t.getSymbol(), 1L, Long::sum);
                }
            }
            String topSymbol = symbolCount.entrySet().stream()
                    .max(Comparator.comparingLong(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse("—");

            entries.add(new LeaderboardEntry(0,
                    portfolio.getUser().getUsername(),
                    totalValue, returnPct, tradeCount, topSymbol));
        }

        entries.sort(Comparator.comparingDouble(LeaderboardEntry::getTotalReturnPct).reversed());

        List<LeaderboardEntry> top20 = entries.stream().limit(20).toList();
        for (int i = 0; i < top20.size(); i++) {
            top20.get(i).setRank(i + 1);
        }
        return top20;
    }
}
