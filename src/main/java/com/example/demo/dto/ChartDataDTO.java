package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public class ChartDataDTO {

    private String symbol;
    private List<BarDTO> bars;
    private BigDecimal currentPrice;
    private BigDecimal openPrice;
    private BigDecimal priceChange;
    private double priceChangePct;
    private BigDecimal high52Week;
    private BigDecimal low52Week;

    public ChartDataDTO() {}

    public ChartDataDTO(String symbol, List<BarDTO> bars, BigDecimal currentPrice,
                        BigDecimal openPrice, BigDecimal priceChange, double priceChangePct,
                        BigDecimal high52Week, BigDecimal low52Week) {
        this.symbol = symbol;
        this.bars = bars;
        this.currentPrice = currentPrice;
        this.openPrice = openPrice;
        this.priceChange = priceChange;
        this.priceChangePct = priceChangePct;
        this.high52Week = high52Week;
        this.low52Week = low52Week;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public List<BarDTO> getBars() { return bars; }
    public void setBars(List<BarDTO> bars) { this.bars = bars; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }

    public BigDecimal getPriceChange() { return priceChange; }
    public void setPriceChange(BigDecimal priceChange) { this.priceChange = priceChange; }

    public double getPriceChangePct() { return priceChangePct; }
    public void setPriceChangePct(double priceChangePct) { this.priceChangePct = priceChangePct; }

    public BigDecimal getHigh52Week() { return high52Week; }
    public void setHigh52Week(BigDecimal high52Week) { this.high52Week = high52Week; }

    public BigDecimal getLow52Week() { return low52Week; }
    public void setLow52Week(BigDecimal low52Week) { this.low52Week = low52Week; }
}
