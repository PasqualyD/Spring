package com.example.demo.controller;

import com.example.demo.service.MarketDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
public class ChartController {

    private final MarketDataService marketDataService;

    @Value("${market.data.mode:simulated}")
    private String marketDataMode;

    public ChartController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/chart")
    public String chart(Model model) {
        return chartForSymbol("AAPL", model);
    }

    @GetMapping("/chart/{symbol}")
    public String chartForSymbol(@PathVariable String symbol, Model model) {
        Set<String> symbolSet = marketDataService.getAllSymbols();
        List<String> symbols = new ArrayList<>(symbolSet);
        symbols.sort(String::compareTo);

        String upper = symbol.toUpperCase();
        String defaultSymbol = symbols.contains(upper)
                ? upper
                : (symbols.isEmpty() ? "AAPL" : symbols.get(0));

        model.addAttribute("symbols", symbols);
        model.addAttribute("defaultSymbol", defaultSymbol);
        model.addAttribute("isSimulated", !"live".equals(marketDataMode));
        return "chart";
    }
}
