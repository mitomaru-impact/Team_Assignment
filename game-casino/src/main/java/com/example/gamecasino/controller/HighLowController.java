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
public class HighLowController {

    private final PlayerRepository playerRepository;
    private final Random random = new Random();

    public HighLowController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // High & Low画面
    @GetMapping("/highlow")
    public String highLow(Model model) {

        Player player = playerRepository.findAll()
                .stream()
                .findFirst()
                .orElse(null);

        int currentCard = random.nextInt(13) + 1;

        model.addAttribute("player", player);
        model.addAttribute("currentCard", currentCard);
        model.addAttribute("currentCardName", cardName(currentCard));

        return "highlow";
    }

    // カードを予想する
    @PostMapping("/highlow/play")
    public String play(
            @RequestParam int bet,
            @RequestParam String choice,
            @RequestParam int currentCard,
            Model model) {

        Player player = playerRepository.findAll()
                .stream()
                .findFirst()
                .orElse(null);

        model.addAttribute("player", player);
        model.addAttribute("currentCard", currentCard);
        model.addAttribute("currentCardName", cardName(currentCard));

        if (player == null) {
            model.addAttribute("error",
                    "先にプレイヤーを登録してください。");
            return "highlow";
        }

        if (bet <= 0 || currentCard < 1 || currentCard > 13
                || (!choice.equals("HIGH") && !choice.equals("LOW"))) {
            model.addAttribute("error", "入力内容を確認してください。");
            return "highlow";
        }

        // 掛け金を支払う。足りない分は借金にする
        if (player.getCoin() >= bet) {
            player.setCoin(player.getCoin() - bet);
        } else {
            int shortage = bet - player.getCoin();
            player.setCoin(0);
            player.setDebt(player.getDebt() + shortage);
        }

        // 次のカードを引く
        int nextCard = random.nextInt(13) + 1;

        int payout = 0;
        String result;

        if (nextCard == currentCard) {
            // 引き分け：掛け金を返す
            payout = bet;
            player.setCoin(player.getCoin() + payout);
            result = "引き分け！掛け金が返ってきました。";

        } else if ((choice.equals("HIGH") && nextCard > currentCard)
                || (choice.equals("LOW") && nextCard < currentCard)) {
            // 勝ち：掛け金を含めて2倍を払い戻す
            payout = bet * 2;
            player.setCoin(player.getCoin() + payout);
            result = "正解！おめでとう！";

        } else {
            // 負け：掛け金を失う
            result = "残念！予想が外れました。";
        }

        playerRepository.save(player);

        model.addAttribute("currentCard", nextCard);
        model.addAttribute("currentCardName", cardName(nextCard));
        model.addAttribute("nextCard", nextCard);
        model.addAttribute("nextCardName", cardName(nextCard));
        model.addAttribute("bet", bet);
        model.addAttribute("payout", payout);
        model.addAttribute("result", result);

        return "highlow";
    }

    // カードの表示名
    private String cardName(int number) {
        if (number == 1) {
            return "A (1)";
        }
        if (number == 11) {
            return "J (11)";
        }
        if (number == 12) {
            return "Q (12)";
        }
        if (number == 13) {
            return "K (13)";
        }
        return String.valueOf(number);
    }
}