package com.example.gamecasino.controller;
 
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gamecasino.model.Player;
import com.example.gamecasino.repository.PlayerRepository;
 
@Controller
public class BlackjackController {
 
    private final PlayerRepository playerRepository;
    private final Random random = new Random();
 
    public BlackjackController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }
 
    @GetMapping("/blackjack")
    public String blackjack(Model model) {
        Player player = getPlayer();
        model.addAttribute("player", player);
        return "blackjack";
    }
 
    @PostMapping("/blackjack/start")
    public String start(@RequestParam int bet, Model model) {
        Player player = getPlayer();
 
        if (player == null) {
            model.addAttribute("error", "先にプレイヤーを登録してください。");
            return "blackjack";
        }
 
        if (bet <= 0) {
            model.addAttribute("error", "掛け金は1以上にしてください。");
            model.addAttribute("player", player);
            return "blackjack";
        }
 
        // 掛け金を支払う。不足分は借金に加算
        if (player.getCoin() >= bet) {
            player.setCoin(player.getCoin() - bet);
        } else {
            int shortage = bet - player.getCoin();
            player.setCoin(0);
            player.setDebt(player.getDebt() + shortage);
        }
 
        List<String> playerCards = new ArrayList<>();
        List<String> dealerCards = new ArrayList<>();
 
        playerCards.add(drawCard());
        playerCards.add(drawCard());
        dealerCards.add(drawCard());
        dealerCards.add(drawCard());
 
        model.addAttribute("bet", bet);
        model.addAttribute("playerScore", score(playerCards));
        model.addAttribute("dealerScore", score(dealerCards));
        model.addAttribute("playing", true);
 
        // 最初の2枚で勝負が決まる場合
        if (score(playerCards) == 21 || score(dealerCards) == 21) {
            return finish(player, playerCards, dealerCards, bet, model);
        }
 
        model.addAttribute("player", player);
        return "blackjack";
    }
 
    @PostMapping("/blackjack/hit")
    public String hit(
            @RequestParam int bet,
            @RequestParam String playerCards,
            @RequestParam String dealerCards,
            Model model) {
 
        Player player = getPlayer();
        if (player == null) {
            model.addAttribute("error", "先にプレイヤーを登録してください。");
            return "blackjack";
        }
 
        List<String> pCards = parseCards(playerCards);
        List<String> dCards = parseCards(dealerCards);
 
        pCards.add(drawCard());
 
        if (score(pCards) >= 21) {
            return finish(player, pCards, dCards, bet, model);
        }
 
        showGame(player, pCards, dCards, bet, model);
        return "blackjack";
    }
 
    @PostMapping("/blackjack/stand")
    public String stand(
            @RequestParam int bet,
            @RequestParam String playerCards,
            @RequestParam String dealerCards,
            Model model) {
 
        Player player = getPlayer();
        if (player == null) {
            model.addAttribute("error", "先にプレイヤーを登録してください。");
            return "blackjack";
        }
 
        List<String> pCards = parseCards(playerCards);
        List<String> dCards = parseCards(dealerCards);
 
        while (score(dCards) < 17) {
            dCards.add(drawCard());
        }
 
        return finish(player, pCards, dCards, bet, model);
    }
 
    private Player getPlayer() {
        return playerRepository.findAll()
                .stream().findFirst().orElse(null);
    }
 
    private String drawCard() {
        String[] ranks = {
                "A", "2", "3", "4", "5", "6", "7",
                "8", "9", "10", "J", "Q", "K"
        };
        return ranks[random.nextInt(ranks.length)];
    }
 
    private int cardValue(String card) {
        if (card.equals("A")) return 1;
        if (card.equals("J") || card.equals("Q") || card.equals("K")) {
            return 10;
        }
        return Integer.parseInt(card);
    }
 
    private int score(List<String> cards) {
        int total = 0;
        int aces = 0;
 
        for (String card : cards) {
            total += cardValue(card);
            if (card.equals("A")) {
                aces++;
            }
        }
 
        // Aは合計が21を超えない場合、11として数える
        while (aces > 0 && total + 10 <= 21) {
            total += 10;
            aces--;
        }
 
        return total;
    }
 
    private List<String> parseCards(String cards) {
 
        List<String> result = new ArrayList<>();
 
        if (cards == null || cards.trim().isEmpty()) {
            return result;
        }
 
        for (String card : cards.split(",")) {
            card = card.trim();
 
            if (!card.isEmpty()) {
                result.add(card);
            }
        }
 
        return result;
    }
    private String toText(List<String> cards) {
        return String.join(",", cards);
    }
 
 
	private void showGame(
	        Player player, List<String> pCards,
	        List<String> dCards, int bet, Model model) {
	
	    model.addAttribute("player", player);
	    model.addAttribute("playerCards", toText(pCards));
	    model.addAttribute("dealerCards", toText(dCards));
	    model.addAttribute("playerScore", score(pCards));
	    model.addAttribute("dealerScore", score(dCards));
	    model.addAttribute("bet", bet);
	    model.addAttribute("playing", true);
	}
 
    private String finish(
            Player player, List<String> pCards,
            List<String> dCards, int bet, Model model) {
 
        int pScore = score(pCards);
        int dScore = score(dCards);
        int payout = 0;
        String result;
 
        if (pScore > 21) {
            result = "バースト！あなたの負けです。";
        } else if (dScore > 21 || pScore > dScore) {
            payout = bet * 2;
            result = "あなたの勝ち！";
        } else if (pScore == dScore) {
            payout = bet;
            result = "引き分け！掛け金が戻ります。";
        } else {
            result = "ディーラーの勝ち！";
        }
 
        player.setCoin(player.getCoin() + payout);
        playerRepository.save(player);
 
        model.addAttribute("player", player);
        model.addAttribute("playerCards", toText(pCards));
        model.addAttribute("dealerCards", toText(dCards));
        model.addAttribute("playerScore", pScore);
        model.addAttribute("dealerScore", dScore);
        model.addAttribute("bet", bet);
        model.addAttribute("payout", payout);
        model.addAttribute("result", result);
        model.addAttribute("playing", false);
 
        return "blackjack";
    }
}