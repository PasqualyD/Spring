package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.MarketDataService;
import com.example.demo.service.PriceAlertService;
import com.example.demo.service.WatchlistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final PriceAlertService priceAlertService;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;

    public WatchlistController(WatchlistService watchlistService,
                               PriceAlertService priceAlertService,
                               UserRepository userRepository,
                               MarketDataService marketDataService) {
        this.watchlistService = watchlistService;
        this.priceAlertService = priceAlertService;
        this.userRepository = userRepository;
        this.marketDataService = marketDataService;
    }

    @GetMapping("/watchlist")
    public String watchlist(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());

        var watchlistItems = watchlistService.getWatchlistWithPrices(user);
        var allAlerts = priceAlertService.getAllAlerts(user);

        List<String> watchedSymbols = new ArrayList<>();
        for (var item : watchlistItems) {
            watchedSymbols.add(item.getSymbol());
        }

        List<String> allSymbols = new ArrayList<>(marketDataService.getAllSymbols());
        allSymbols.sort(String::compareTo);

        List<String> availableSymbols = new ArrayList<>(allSymbols);
        availableSymbols.removeAll(watchedSymbols);

        model.addAttribute("watchlistItems", watchlistItems);
        model.addAttribute("alerts", allAlerts);
        model.addAttribute("availableSymbols", availableSymbols);
        model.addAttribute("watchedSymbols", watchedSymbols);

        return "watchlist";
    }

    @PostMapping("/watchlist/add")
    public String addToWatchlist(@RequestParam String symbol,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(principal.getName());
        try {
            watchlistService.addToWatchlist(user, symbol);
            redirectAttributes.addFlashAttribute("successMessage",
                    symbol.toUpperCase() + " added to your watchlist");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/watchlist";
    }

    @PostMapping("/watchlist/remove")
    public String removeFromWatchlist(@RequestParam String symbol,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(principal.getName());
        watchlistService.removeFromWatchlist(user, symbol);
        return "redirect:/watchlist";
    }

    @PostMapping("/watchlist/alert")
    public String createAlert(@RequestParam String symbol,
                              @RequestParam BigDecimal targetPrice,
                              @RequestParam String direction,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(principal.getName());
        try {
            priceAlertService.createAlert(user, symbol, targetPrice, direction);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Alert set: " + symbol.toUpperCase() + " " + direction + " $" + targetPrice);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/watchlist";
    }

    @PostMapping("/watchlist/alert/delete")
    public String deleteAlert(@RequestParam Long alertId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(principal.getName());
        priceAlertService.deleteAlert(alertId, user);
        return "redirect:/watchlist";
    }
}
