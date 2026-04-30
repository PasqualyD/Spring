package com.example.demo.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for trade events from Kafka
 */
public class TradeEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String tradeId;
    private String symbol;
    private int quantity;
    private double price;
    private String side;
    private long timestamp;
    private String correlationId;
    
    public TradeEvent() {
    }
    
    public TradeEvent(String tradeId, String symbol, int quantity, double price, 
                     String side, long timestamp, String correlationId) {
        this.tradeId = tradeId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }
    
    public String getTradeId() {
        return tradeId;
    }
    
    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public String getSide() {
        return side;
    }
    
    public void setSide(String side) {
        this.side = side;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    @Override
    public String toString() {
        return "TradeEvent{" +
                "tradeId='" + tradeId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", side='" + side + '\'' +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
