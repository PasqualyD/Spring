package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WatchlistItemDTO {

    private String symbol;
    private LocalDateTime addedAt;
    private BigDecimal priceAtAdd;
    private BigDecimal currentPrice;
    private BigDecimal priceChange;
    private BigDecimal priceChangePct;
    private boolean owned;

    public WatchlistItemDTO() {}

    public WatchlistItemDTO(String symbol, LocalDateTime addedAt, BigDecimal priceAtAdd,
                            BigDecimal currentPrice, BigDecimal priceChange,
                            BigDecimal priceChangePct, boolean owned) {
        this.symbol = symbol;
        this.addedAt = addedAt;
        this.priceAtAdd = priceAtAdd;
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.priceChangePct = priceChangePct;
        this.owned = owned;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public BigDecimal getPriceAtAdd() { return priceAtAdd; }
    public void setPriceAtAdd(BigDecimal priceAtAdd) { this.priceAtAdd = priceAtAdd; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getPriceChange() { return priceChange; }
    public void setPriceChange(BigDecimal priceChange) { this.priceChange = priceChange; }

    public BigDecimal getPriceChangePct() { return priceChangePct; }
    public void setPriceChangePct(BigDecimal priceChangePct) { this.priceChangePct = priceChangePct; }

    public boolean isOwned() { return owned; }
    public void setOwned(boolean owned) { this.owned = owned; }
}
