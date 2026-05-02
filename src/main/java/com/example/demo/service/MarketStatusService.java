package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MarketStatusService {

    private static final Logger log = LoggerFactory.getLogger(MarketStatusService.class);
    private static final ZoneId ET = ZoneId.of("America/New_York");
    private static final LocalTime OPEN_TIME  = LocalTime.of(9, 30);
    private static final LocalTime CLOSE_TIME = LocalTime.of(16, 0);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a z");

    @Value("${market.data.mode:simulated}")
    private String mode;

    @Autowired(required = false)
    private AlpacaTradeService alpacaTradeService;

    private volatile boolean marketOpen = false;
    private volatile String nextEvent = "";

    @Scheduled(fixedRate = 60_000, initialDelay = 0)
    public void refreshMarketStatus() {
        try {
            if ("live".equals(mode) && alpacaTradeService != null) {
                marketOpen = alpacaTradeService.isMarketOpen();
            } else {
                marketOpen = calculateSimulatedStatus();
            }
            nextEvent = calculateNextEvent();
        } catch (Exception e) {
            log.warn("MarketStatusService refresh failed: {}", e.getMessage());
        }
    }

    public boolean isMarketOpen() { return marketOpen; }
    public String getNextEvent()  { return nextEvent; }

    private boolean calculateSimulatedStatus() {
        ZonedDateTime now = ZonedDateTime.now(ET);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;
        LocalTime time = now.toLocalTime();
        return !time.isBefore(OPEN_TIME) && time.isBefore(CLOSE_TIME);
    }

    private String calculateNextEvent() {
        ZonedDateTime now = ZonedDateTime.now(ET);
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        boolean isWeekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        if (isWeekday && time.isBefore(OPEN_TIME)) {
            return "Opens " + now.with(OPEN_TIME).format(TIME_FMT);
        }
        if (isWeekday && !time.isBefore(OPEN_TIME) && time.isBefore(CLOSE_TIME)) {
            return "Closes " + now.with(CLOSE_TIME).format(TIME_FMT);
        }
        // Find next Monday (or next day) open
        ZonedDateTime next = now.plusDays(1).with(OPEN_TIME);
        while (next.getDayOfWeek() == DayOfWeek.SATURDAY || next.getDayOfWeek() == DayOfWeek.SUNDAY) {
            next = next.plusDays(1);
        }
        return "Opens " + next.format(TIME_FMT);
    }
}
