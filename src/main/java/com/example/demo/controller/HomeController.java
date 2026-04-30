package com.example.demo.controller;

import com.example.demo.dto.RegistrationForm;
import com.example.demo.model.Trade;
import com.example.demo.model.User;
import com.example.demo.repository.TradeRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.MarketDataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final MarketDataService marketDataService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public HomeController(UserRepository userRepository,
                          TradeRepository tradeRepository,
                          MarketDataService marketDataService,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.tradeRepository = tradeRepository;
        this.marketDataService = marketDataService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("message", "Welcome");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        if (authenticated) {
            model.addAttribute("allUsers", userRepository.findAll());
        }
        return "home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("message", "About");
        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("message", "Contact");
        return "contact";
    }

    @GetMapping("/login")
    public String login(Model model, @RequestParam(required = false) String error) {
        model.addAttribute("message", "Login");
        model.addAttribute("loginError", error != null);
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("message", "Register");
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("message", "Dashboard");

        if (principal != null) {
            String username = principal.getName();
            User user = userRepository.findByUsername(username);
            if (user != null) {
                model.addAttribute("currentUser", user);
                List<Trade> trades = tradeRepository.findByUserOrderByExecutedAtDesc(user);

                Map<String, Integer> holdings = new HashMap<>();
                double totalCost = 0;

                for (Trade trade : trades) {
                    if (!"FILLED".equalsIgnoreCase(trade.getStatus())) {
                        continue;
                    }
                    int delta = "SELL".equalsIgnoreCase(trade.getSide()) ? -trade.getQuantity() : trade.getQuantity();
                    holdings.merge(trade.getSymbol(), delta, Integer::sum);
                    totalCost += "SELL".equalsIgnoreCase(trade.getSide()) ? -trade.getQuantity() * trade.getPrice() : trade.getQuantity() * trade.getPrice();
                }

                List<PortfolioPosition> positions = new ArrayList<>();
                double currentValue = 0;
                for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
                    if (entry.getValue() == 0) {
                        continue;
                    }
                    double price = marketDataService.getPrice(entry.getKey()).orElse(0.0);
                    double positionValue = price * entry.getValue();
                    currentValue += positionValue;
                    positions.add(new PortfolioPosition(entry.getKey(), entry.getValue(), price, positionValue));
                }

                double pnl = currentValue - totalCost;
                user.setPortfolioValue(currentValue);
                userRepository.save(user);

                model.addAttribute("positions", positions);
                model.addAttribute("portfolioValue", currentValue);
                model.addAttribute("portfolioPnl", pnl);
                model.addAttribute("tradeCount", trades.size());
            }
        }

        return "dashboard";
    }

    @GetMapping("/dashboard/trades")
    public String tradeHistory(Model model, Principal principal) {
        model.addAttribute("message", "My Trades");
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName());
            if (user != null) {
                model.addAttribute("tradeHistory", tradeRepository.findByUserOrderByExecutedAtDesc(user));
            }
        }
        return "trades";
    }

    @PostMapping("/register")
    public String registerUser(@Valid RegistrationForm registrationForm,
                               BindingResult bindingResult,
                               Model model,
                               HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("message", "Register");
            model.addAttribute("registrationForm", registrationForm);
            return "register";
        }

        User existing = userRepository.findByUsername(registrationForm.getEmail());
        if (existing != null) {
            bindingResult.rejectValue("email", "error.email", "Email is already registered");
            model.addAttribute("message", "Register");
            model.addAttribute("registrationForm", registrationForm);
            return "register";
        }

        User newUser = new User();
        newUser.setUsername(registrationForm.getEmail());
        newUser.setPassword(passwordEncoder.encode(registrationForm.getPassword()));
        newUser.setRole("ROLE_USER");
        userRepository.save(newUser);

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(registrationForm.getEmail(), registrationForm.getPassword());
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        return "redirect:/dashboard";
    }

    public static class PortfolioPosition {
        private final String symbol;
        private final int quantity;
        private final double currentPrice;
        private final double value;

        public PortfolioPosition(String symbol, int quantity, double currentPrice, double value) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.currentPrice = currentPrice;
            this.value = value;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public double getValue() {
            return value;
        }
    }
}
