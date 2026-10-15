package com.example.gamecasino.controller;

import java.util.Random;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gamecasino.model.Player;
import com.example.gamecasino.repository.PlayerRepository;

@Controller
public class SlotController {

    private final PlayerRepository playerRepository;

    public SlotController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @GetMapping("/slot")
    public String slot(Model model) {

        Player player = playerRepository.findAll()
                .stream()
                .findFirst()
                .orElse(null);

        model.addAttribute("player", player);

        return "slot";
    }

    @PostMapping("/slot/play")
    public String playSlot(
            @RequestParam int bet,
            Model model) {

        Player player = playerRepository.findAll()
                .stream()
                .findFirst()
                .orElse(null);

        // 賭け金チェック
        if (bet <= 0) {
            model.addAttribute("player", player);
            model.addAttribute("error", "賭け金は1C以上にしてください。");
            return "slot";
        }

        // -------------------------
        // 賭け金を支払う
        // -------------------------

        if (player.getCoin() >= bet) {

            // Coinだけで賭けられる
            player.setCoin(player.getCoin() - bet);

        } else {

            // Coinが足りない場合
            int shortage = bet - player.getCoin();

            // Coinを0にする
            player.setCoin(0);

            // 足りない分をDebtに追加
            player.setDebt(player.getDebt() + shortage);
        }

        // -------------------------
        // スロット抽選
        // -------------------------

        String[] symbols = {
                "🍒",
                "🍋",
                "🍊",
                "BAR",
                "7"
        };

        Random random = new Random();

        String result1 = symbols[random.nextInt(symbols.length)];
        String result2 = symbols[random.nextInt(symbols.length)];
        String result3 = symbols[random.nextInt(symbols.length)];

        int multiplier = 0;

        // 3つ全部同じか確認
        if (result1.equals(result2)
                && result2.equals(result3)) {

            if (result1.equals("7")) {

                multiplier = 50;

            } else if (result1.equals("BAR")) {

                multiplier = 20;

            } else if (result1.equals("🍒")) {

                multiplier = 10;

            } else {

                multiplier = 5;
            }
        }

        // -------------------------
        // 配当
        // -------------------------

        int payout = bet * multiplier;

        // 配当をCoinに追加
        player.setCoin(player.getCoin() + payout);

        // H2に保存
        playerRepository.save(player);

        // -------------------------
        // 画面に渡す
        // -------------------------

        model.addAttribute("player", player);

        model.addAttribute("result1", result1);
        model.addAttribute("result2", result2);
        model.addAttribute("result3", result3);

        model.addAttribute("bet", bet);
        model.addAttribute("multiplier", multiplier);
        model.addAttribute("payout", payout);

        return "slot";
    }
}