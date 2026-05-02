package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public class ChartDataDTO {

    private String symbol;
    private List<BarDTO> bars;
    private BigDecimal currentPrice;
    private BigDecimal priceChange;
    private double priceChangePct;

    public ChartDataDTO() {}

    public ChartDataDTO(String symbol, List<BarDTO> bars, BigDecimal currentPrice,
                        BigDecimal priceChange, double priceChangePct) {
        this.symbol = symbol;
        this.bars = bars;
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.priceChangePct = priceChangePct;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public List<BarDTO> getBars() { return bars; }
    public void setBars(List<BarDTO> bars) { this.bars = bars; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getPriceChange() { return priceChange; }
    public void setPriceChange(BigDecimal priceChange) { this.priceChange = priceChange; }

    public double getPriceChangePct() { return priceChangePct; }
    public void setPriceChangePct(double priceChangePct) { this.priceChangePct = priceChangePct; }
}
