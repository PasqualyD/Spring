package com.example.demo.repository;

import com.example.demo.model.Portfolio;
import com.example.demo.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByPortfolioAndSymbol(Portfolio portfolio, String symbol);
    List<Position> findByPortfolio(Portfolio portfolio);
}
