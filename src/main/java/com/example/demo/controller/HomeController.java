package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public HomeController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
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
    public String login(Model model) {
        model.addAttribute("message", "Login");
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("message", "Register");
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("message", "Dashboard");
        return "dashboard";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username, @RequestParam String password, HttpServletRequest request) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole("ROLE_USER");
        userRepository.save(newUser);

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(token);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        return "redirect:/dashboard";
    }
}
