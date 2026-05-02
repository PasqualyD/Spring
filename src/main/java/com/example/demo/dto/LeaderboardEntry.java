package com.example.demo.dto;

import java.math.BigDecimal;

public class LeaderboardEntry {

    private int rank;
    private String username;
    private BigDecimal totalPortfolioValue;
    private double totalReturnPct;
    private int totalTrades;
    private String topSymbol;

    public LeaderboardEntry() {}

    public LeaderboardEntry(int rank, String username, BigDecimal totalPortfolioValue,
                            double totalReturnPct, int totalTrades, String topSymbol) {
        this.rank = rank;
        this.username = username;
        this.totalPortfolioValue = totalPortfolioValue;
        this.totalReturnPct = totalReturnPct;
        this.totalTrades = totalTrades;
        this.topSymbol = topSymbol;
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
    public void setTotalPortfolioValue(BigDecimal totalPortfolioValue) { this.totalPortfolioValue = totalPortfolioValue; }

    public double getTotalReturnPct() { return totalReturnPct; }
    public void setTotalReturnPct(double totalReturnPct) { this.totalReturnPct = totalReturnPct; }

    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int totalTrades) { this.totalTrades = totalTrades; }

    public String getTopSymbol() { return topSymbol; }
    public void setTopSymbol(String topSymbol) { this.topSymbol = topSymbol; }
}
