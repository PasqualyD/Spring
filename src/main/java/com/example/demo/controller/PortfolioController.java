package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.MarketDataService;
import com.example.demo.service.PortfolioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;

    public PortfolioController(PortfolioService portfolioService,
                               UserRepository userRepository,
                               MarketDataService marketDataService) {
        this.portfolioService = portfolioService;
        this.userRepository = userRepository;
        this.marketDataService = marketDataService;
    }

    @PostMapping("/trade")
    public String executeTrade(@RequestParam String symbol,
                               @RequestParam String side,
                               @RequestParam BigDecimal quantity,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(principal.getName());
        PortfolioService.TradeResult result = portfolioService.executeTrade(user, symbol, side, quantity);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("successMessage", "Trade executed: " + result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Trade rejected: " + result.getMessage());
        }
        return "redirect:/portfolio";
    }

    @GetMapping("/portfolio")
    public String portfolio(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());

        PortfolioService.PortfolioSummary summary = portfolioService.getPortfolioSummary(user);
        List<PortfolioService.PositionWithValue> positions = portfolioService.getPositionsWithValue(user);
        List<?> recentTrades = portfolioService.getRecentTrades(user, 20);

        List<String> sortedSymbols = new ArrayList<>(marketDataService.getAllSymbols());
        sortedSymbols.sort(String::compareTo);

        Map<String, Double> allPrices = new HashMap<>();
        for (String sym : sortedSymbols) {
            marketDataService.getPrice(sym).ifPresent(p -> allPrices.put(sym, p));
        }

        model.addAttribute("summary", summary);
        model.addAttribute("positions", positions);
        model.addAttribute("recentTrades", recentTrades);
        model.addAttribute("allSymbols", sortedSymbols);
        model.addAttribute("allPrices", allPrices);
        model.addAttribute("metrics", portfolioService.getMetrics(user));

        return "portfolio";
    }
}
