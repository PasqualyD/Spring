package com.example.demo.controller;

import com.example.demo.service.MarketStatusService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributeAdvice {

    private final MarketStatusService marketStatusService;

    public GlobalModelAttributeAdvice(MarketStatusService marketStatusService) {
        this.marketStatusService = marketStatusService;
    }

    @ModelAttribute("marketOpen")
    public boolean marketOpen() {
        return marketStatusService.isMarketOpen();
    }

    @ModelAttribute("marketNextEvent")
    public String marketNextEvent() {
        return marketStatusService.getNextEvent();
    }
}
