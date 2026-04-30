package com.example.demo.model;

public class Trade {
    private String symbol;
    private int quantity;
    private double price;
    private String side;

    public Trade() {
    }

    public Trade(String symbol, int quantity, double price, String side) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
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

    @Override
    public String toString() {
        return "Trade{" +
                "symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", side='" + side + '\'' +
                '}';
    }
}
