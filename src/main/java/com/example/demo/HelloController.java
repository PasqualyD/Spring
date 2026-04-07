package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List; // Add this import for the list of users


@Controller
public class HelloController {

// 1. The field is declared as final
    private final UserRepository userRepository;

    // 2. The Constructor: This is what fixes your error.
    // Spring sees this and automatically provides the UserRepository.
    public HelloController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
@GetMapping({"/", "/home"})
    public String home(@RequestParam(name = "name", required = false) String name, Model model) {
        if (name != null) {
            // Logic: If a name is in the URL, save it to Postgres!
            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(name.toLowerCase() + "@example.com");
            userRepository.save(newUser);
        }
        
        model.addAttribute("message", "Welcome");
        model.addAttribute("name", name);
        // Add the list of all users from the DB to the page
        model.addAttribute("allUsers", userRepository.findAll()); 
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

    @PostMapping("/login")
    public String register(@RequestParam("email") String email,
                           @RequestParam("password") String password) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(password);
        userRepository.save(newUser);
        return "redirect:/";
    }
}
