package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByUserOrderByAddedAtDesc(User user);
    Optional<WatchlistItem> findByUserAndSymbol(User user, String symbol);
    boolean existsByUserAndSymbol(User user, String symbol);

    @Transactional
    void deleteByUserAndSymbol(User user, String symbol);
}
