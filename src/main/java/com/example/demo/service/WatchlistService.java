package com.example.demo.service;

import com.example.demo.dto.WatchlistItemDTO;
import com.example.demo.model.Portfolio;
import com.example.demo.model.User;
import com.example.demo.model.WatchlistItem;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.PositionRepository;
import com.example.demo.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final MarketDataService marketDataService;
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            MarketDataService marketDataService,
                            PortfolioRepository portfolioRepository,
                            PositionRepository positionRepository) {
        this.watchlistRepository = watchlistRepository;
        this.marketDataService = marketDataService;
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
    }

    public List<WatchlistItem> getWatchlist(User user) {
        return watchlistRepository.findByUserOrderByAddedAtDesc(user);
    }

    public WatchlistItem addToWatchlist(User user, String symbol) {
        String upper = symbol.toUpperCase();
        if (marketDataService.getPrice(upper).isEmpty()) {
            throw new IllegalArgumentException("Unknown symbol: " + upper);
        }
        if (watchlistRepository.existsByUserAndSymbol(user, upper)) {
            throw new IllegalStateException(upper + " is already in your watchlist");
        }
        double rawPrice = marketDataService.getPrice(upper).get();
        WatchlistItem item = new WatchlistItem();
        item.setUser(user);
        item.setSymbol(upper);
        item.setPriceAtAdd(BigDecimal.valueOf(rawPrice).setScale(4, RoundingMode.HALF_UP));
        return watchlistRepository.save(item);
    }

    public void removeFromWatchlist(User user, String symbol) {
        watchlistRepository.deleteByUserAndSymbol(user, symbol.toUpperCase());
    }

    public List<WatchlistItemDTO> getWatchlistWithPrices(User user) {
        List<WatchlistItem> items = watchlistRepository.findByUserOrderByAddedAtDesc(user);
        Optional<Portfolio> portfolioOpt = portfolioRepository.findByUser(user);

        List<WatchlistItemDTO> result = new ArrayList<>();
        for (WatchlistItem item : items) {
            double rawPrice = marketDataService.getPrice(item.getSymbol())
                    .orElse(item.getPriceAtAdd() != null ? item.getPriceAtAdd().doubleValue() : 0.0);
            BigDecimal currentPrice = BigDecimal.valueOf(rawPrice).setScale(4, RoundingMode.HALF_UP);
            BigDecimal priceAtAdd = item.getPriceAtAdd() != null
                    ? item.getPriceAtAdd() : currentPrice;
            BigDecimal priceChange = currentPrice.subtract(priceAtAdd).setScale(4, RoundingMode.HALF_UP);
            BigDecimal priceChangePct = priceAtAdd.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : priceChange.divide(priceAtAdd, 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

            boolean owned = portfolioOpt.map(portfolio ->
                    positionRepository.findByPortfolioAndSymbol(portfolio, item.getSymbol()).isPresent()
            ).orElse(false);

            result.add(new WatchlistItemDTO(item.getSymbol(), item.getAddedAt(),
                    priceAtAdd, currentPrice, priceChange, priceChangePct, owned));
        }
        return result;
    }
}
