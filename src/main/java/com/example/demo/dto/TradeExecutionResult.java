package com.example.demo.dto;

public class TradeExecutionResult {
    private String executionId;
    private String symbol;
    private int executedQuantity;
    private double executedPrice;
    private String side;
    private String status;
    private long timestamp;

    public TradeExecutionResult() {
    }

    public TradeExecutionResult(String executionId, String symbol, int executedQuantity, 
                               double executedPrice, String side, String status, long timestamp) {
        this.executionId = executionId;
        this.symbol = symbol;
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
        this.side = side;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getExecutedQuantity() {
        return executedQuantity;
    }

    public void setExecutedQuantity(int executedQuantity) {
        this.executedQuantity = executedQuantity;
    }

    public double getExecutedPrice() {
        return executedPrice;
    }

    public void setExecutedPrice(double executedPrice) {
        this.executedPrice = executedPrice;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TradeExecutionResult{" +
                "executionId='" + executionId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", executedQuantity=" + executedQuantity +
                ", executedPrice=" + executedPrice +
                ", side='" + side + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
