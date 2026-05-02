package com.example.demo.controller;

import com.example.demo.model.Portfolio;
import com.example.demo.model.User;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.PositionRepository;
import com.example.demo.repository.TradeRecordRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AlpacaTradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
public class SettingsController {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TradeRecordRepository tradeRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${market.data.mode:simulated}")
    private String marketDataMode;

    @Autowired(required = false)
    private AlpacaTradeService alpacaTradeService;

    public SettingsController(UserRepository userRepository,
                              PortfolioRepository portfolioRepository,
                              PositionRepository positionRepository,
                              TradeRecordRepository tradeRecordRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.tradeRecordRepository = tradeRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/settings")
    public String settings(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("isLiveMode", "live".equals(marketDataMode));
        if ("live".equals(marketDataMode) && alpacaTradeService != null) {
            try {
                model.addAttribute("alpacaAccount", alpacaTradeService.getAccountInfo());
            } catch (Exception ignored) {}
        }
        return "settings";
    }

    @PostMapping("/settings/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(principal.getName());
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect");
            return "redirect:/settings";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match");
            return "redirect:/settings";
        }
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 6 characters");
            return "redirect:/settings";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully");
        return "redirect:/settings";
    }

    @PostMapping("/settings/reset-portfolio")
    @Transactional
    public String resetPortfolio(@RequestParam String confirmation,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (!"RESET".equals(confirmation)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Type RESET to confirm");
            return "redirect:/settings";
        }
        User user = userRepository.findByUsername(principal.getName());
        Portfolio portfolio = portfolioRepository.findByUser(user).orElse(null);
        if (portfolio == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Portfolio not found");
            return "redirect:/settings";
        }
        tradeRecordRepository.deleteAll(tradeRecordRepository.findByPortfolioOrderByExecutedAtDesc(portfolio));
        positionRepository.deleteAll(positionRepository.findByPortfolio(portfolio));
        portfolio.setCashBalance(new BigDecimal("100000.00"));
        portfolioRepository.save(portfolio);
        redirectAttributes.addFlashAttribute("successMessage", "Portfolio reset to $100,000 cash");
        return "redirect:/settings";
    }
}
