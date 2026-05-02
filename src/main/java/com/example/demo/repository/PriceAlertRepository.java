package com.example.demo.repository;

import com.example.demo.model.PriceAlert;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByUserOrderByCreatedAtDesc(User user);
    List<PriceAlert> findBySymbolAndTriggeredFalse(String symbol);
    List<PriceAlert> findByUserAndTriggeredFalse(User user);
}
