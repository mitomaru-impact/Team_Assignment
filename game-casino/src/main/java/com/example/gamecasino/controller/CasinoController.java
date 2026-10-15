package com.example.gamecasino.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gamecasino.model.Player;
import com.example.gamecasino.repository.PlayerRepository;

@Controller
public class CasinoController {

    private final PlayerRepository playerRepository;

    public CasinoController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Game Casino");
        return "index";
    }

    @GetMapping("/player")
    public String playerPage() {
        return "player";
    }

    @PostMapping("/player/start")
    public String startGame(
            @RequestParam String playerName,
            Model model) {

        Player player = new Player(playerName);

        // H2データベースに保存
        playerRepository.save(player);

        model.addAttribute("player", player);

        return "home";
    }
}