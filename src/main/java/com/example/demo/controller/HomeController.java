package com.example.demo.controller;

import com.example.demo.dto.RegistrationForm;
import com.example.demo.model.Portfolio;
import com.example.demo.model.User;
import com.example.demo.repository.PortfolioRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.MarketDataService;
import com.example.demo.service.PortfolioService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketDataService marketDataService;
    private final PortfolioService portfolioService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public HomeController(UserRepository userRepository,
                          PortfolioRepository portfolioRepository,
                          MarketDataService marketDataService,
                          PortfolioService portfolioService,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketDataService = marketDataService;
        this.portfolioService = portfolioService;
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
            User user = userRepository.findByUsername(principal.getName());
            if (user != null) {
                model.addAttribute("currentUser", user);

                PortfolioService.PortfolioSummary summary = portfolioService.getPortfolioSummary(user);
                List<PortfolioService.PositionWithValue> positions = portfolioService.getPositionsWithValue(user);

                model.addAttribute("summary", summary);
                model.addAttribute("positions", positions);
                model.addAttribute("portfolioValue", summary.getTotalPortfolioValue());
                model.addAttribute("portfolioPnl", summary.getTotalPortfolioValue()
                        .subtract(summary.getCashBalance().add(summary.getTotalPositionValue())
                                .subtract(summary.getTotalPortfolioValue())));
                model.addAttribute("tradeCount", portfolioService.getRecentTrades(user, 1000).size());

                // Market snapshot: top 5 symbols with live prices
                String[] snapSymbols = {"AAPL", "TSLA", "MSFT", "NVDA", "AMZN"};
                Map<String, Double> marketPrices = new HashMap<>();
                for (String sym : snapSymbols) {
                    marketDataService.getPrice(sym).ifPresent(p -> marketPrices.put(sym, p));
                }
                model.addAttribute("marketPrices", marketPrices);
                model.addAttribute("snapSymbols", snapSymbols);
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
                model.addAttribute("tradeHistory", portfolioService.getRecentTrades(user, 1000));
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

        Portfolio portfolio = new Portfolio();
        portfolio.setUser(newUser);
        portfolioRepository.save(portfolio);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(registrationForm.getEmail(), registrationForm.getPassword());
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        return "redirect:/dashboard";
    }
}
