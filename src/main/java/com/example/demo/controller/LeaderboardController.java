package com.example.demo.controller;

import com.example.demo.dto.LeaderboardEntry;
import com.example.demo.service.LeaderboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model, Principal principal) {
        List<LeaderboardEntry> entries = leaderboardService.getLeaderboard();
        model.addAttribute("entries", entries);

        if (principal != null) {
            model.addAttribute("currentUsername", principal.getName());
        }
        return "leaderboard";
    }
}
