package com.example.demo.repository;

import com.example.demo.model.Portfolio;
import com.example.demo.model.TradeRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRecordRepository extends JpaRepository<TradeRecord, Long> {
    List<TradeRecord> findByPortfolioOrderByExecutedAtDesc(Portfolio portfolio);
    List<TradeRecord> findByPortfolioOrderByExecutedAtDesc(Portfolio portfolio, Pageable pageable);
}
