package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"portfolio_id", "symbol"})
})
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal averageCostBasis;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        openedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Portfolio getPortfolio() { return portfolio; }
    public void setPortfolio(Portfolio portfolio) { this.portfolio = portfolio; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getAverageCostBasis() { return averageCostBasis; }
    public void setAverageCostBasis(BigDecimal averageCostBasis) { this.averageCostBasis = averageCostBasis; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
