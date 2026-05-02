package com.example.demo.dto;

import java.math.BigDecimal;

public class AlpacaOrderResult {

    private String alpacaOrderId;
    private String symbol;
    private String side;
    private String status;
    private BigDecimal filledQty;
    private BigDecimal filledAvgPrice;
    private String submittedAt;
    private String errorMessage;

    public AlpacaOrderResult() {}

    public String getAlpacaOrderId() { return alpacaOrderId; }
    public void setAlpacaOrderId(String alpacaOrderId) { this.alpacaOrderId = alpacaOrderId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getFilledQty() { return filledQty; }
    public void setFilledQty(BigDecimal filledQty) { this.filledQty = filledQty; }

    public BigDecimal getFilledAvgPrice() { return filledAvgPrice; }
    public void setFilledAvgPrice(BigDecimal filledAvgPrice) { this.filledAvgPrice = filledAvgPrice; }

    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
