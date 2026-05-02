package com.example.demo.dto;

import java.math.BigDecimal;

public class TradingMetrics {

    private int totalTrades;
    private int totalBuys;
    private int totalSells;
    private BigDecimal totalVolumeTraded;
    private double winRate;
    private BigDecimal bestTrade;
    private BigDecimal worstTrade;
    private BigDecimal averageTradeSize;
    private String mostTradedSymbol;

    public TradingMetrics() {}

    public TradingMetrics(int totalTrades, int totalBuys, int totalSells,
                          BigDecimal totalVolumeTraded, double winRate,
                          BigDecimal bestTrade, BigDecimal worstTrade,
                          BigDecimal averageTradeSize, String mostTradedSymbol) {
        this.totalTrades = totalTrades;
        this.totalBuys = totalBuys;
        this.totalSells = totalSells;
        this.totalVolumeTraded = totalVolumeTraded;
        this.winRate = winRate;
        this.bestTrade = bestTrade;
        this.worstTrade = worstTrade;
        this.averageTradeSize = averageTradeSize;
        this.mostTradedSymbol = mostTradedSymbol;
    }

    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int totalTrades) { this.totalTrades = totalTrades; }

    public int getTotalBuys() { return totalBuys; }
    public void setTotalBuys(int totalBuys) { this.totalBuys = totalBuys; }

    public int getTotalSells() { return totalSells; }
    public void setTotalSells(int totalSells) { this.totalSells = totalSells; }

    public BigDecimal getTotalVolumeTraded() { return totalVolumeTraded; }
    public void setTotalVolumeTraded(BigDecimal totalVolumeTraded) { this.totalVolumeTraded = totalVolumeTraded; }

    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }

    public BigDecimal getBestTrade() { return bestTrade; }
    public void setBestTrade(BigDecimal bestTrade) { this.bestTrade = bestTrade; }

    public BigDecimal getWorstTrade() { return worstTrade; }
    public void setWorstTrade(BigDecimal worstTrade) { this.worstTrade = worstTrade; }

    public BigDecimal getAverageTradeSize() { return averageTradeSize; }
    public void setAverageTradeSize(BigDecimal averageTradeSize) { this.averageTradeSize = averageTradeSize; }

    public String getMostTradedSymbol() { return mostTradedSymbol; }
    public void setMostTradedSymbol(String mostTradedSymbol) { this.mostTradedSymbol = mostTradedSymbol; }
}
