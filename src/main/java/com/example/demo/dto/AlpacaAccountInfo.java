package com.example.demo.dto;

import java.math.BigDecimal;

public class AlpacaAccountInfo {

    private BigDecimal buyingPower;
    private BigDecimal portfolioValue;
    private BigDecimal equity;
    private BigDecimal cash;
    private String accountStatus;

    public AlpacaAccountInfo() {}

    public BigDecimal getBuyingPower() { return buyingPower; }
    public void setBuyingPower(BigDecimal buyingPower) { this.buyingPower = buyingPower; }

    public BigDecimal getPortfolioValue() { return portfolioValue; }
    public void setPortfolioValue(BigDecimal portfolioValue) { this.portfolioValue = portfolioValue; }

    public BigDecimal getEquity() { return equity; }
    public void setEquity(BigDecimal equity) { this.equity = equity; }

    public BigDecimal getCash() { return cash; }
    public void setCash(BigDecimal cash) { this.cash = cash; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
}
