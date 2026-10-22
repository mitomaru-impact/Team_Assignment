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

    // トップページ
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Game Casino");
        return "index";
    }

    // プレイヤー登録画面
    @GetMapping("/player")
    public String playerPage() {
        return "player";
    }

    // プレイヤー登録処理
    @PostMapping("/player/start")
    public String startGame(
            @RequestParam String playerName,
            Model model) {

        // 名前の入力チェック
        if (playerName == null || playerName.trim().isEmpty()) {
            model.addAttribute("error", "プレイヤー名を入力してください。");
            return "player";
        }

        Player player = new Player(playerName.trim());

        // H2データベースに保存
        playerRepository.save(player);

        model.addAttribute("player", player);

        return "home";
    }
}