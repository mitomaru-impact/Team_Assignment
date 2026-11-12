package com.example.gamecasino.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jakarta.servlet.http.HttpSession;

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

    // ブラックジャック画面
    @GetMapping("/blackjack")
    public String blackjack(Model model, HttpSession session) {

        Player player = getPlayer();

        model.addAttribute("player", player);

        List<String> pCards = getSessionCards(session, "blackjackPlayerCards");
        List<String> dCards = getSessionCards(session, "blackjackDealerCards");

        Object betObject = session.getAttribute("blackjackBet");

        if (!pCards.isEmpty() && !dCards.isEmpty()
                && betObject instanceof Integer) {

            showGame(player, pCards, dCards, (Integer) betObject, model);

        } else {
            model.addAttribute("playing", false);
        }

        return "blackjack";
    }

    // ゲーム開始
    @PostMapping("/blackjack/start")
    public String start(
            @RequestParam int bet,
            Model model,
            HttpSession session) {

        Player player = getPlayer();

        if (player == null) {
            model.addAttribute("error", "先にプレイヤーを登録してください。");
            model.addAttribute("playing", false);
            return "blackjack";
        }

        if (bet <= 0) {
            model.addAttribute("error", "掛け金は1以上にしてください。");
            model.addAttribute("player", player);
            model.addAttribute("playing", false);
            return "blackjack";
        }

        // 前のゲーム情報を削除
        clearGame(session);

        // 掛け金を支払う。不足分は借金に加算
        if (player.getCoin() >= bet) {
            player.setCoin(player.getCoin() - bet);
        } else {
            int shortage = bet - player.getCoin();
            player.setCoin(0);
            player.setDebt(player.getDebt() + shortage);
        }

        playerRepository.save(player);

        // カードを配る
        List<String> playerCards = new ArrayList<>();
        List<String> dealerCards = new ArrayList<>();

        playerCards.add(drawCard());
        playerCards.add(drawCard());

        dealerCards.add(drawCard());
        dealerCards.add(drawCard());

        // カード情報はサーバー側のセッションに保存
        session.setAttribute("blackjackPlayerCards", playerCards);
        session.setAttribute("blackjackDealerCards", dealerCards);
        session.setAttribute("blackjackBet", bet);

        // 最初の2枚で勝負が決まる場合
        if (score(playerCards) == 21 || score(dealerCards) == 21) {
            return finish(player, playerCards, dealerCards, bet, model, session);
        }

        showGame(player, playerCards, dealerCards, bet, model);

        return "blackjack";
    }

    // HIT：プレイヤーにカードを1枚追加
    @PostMapping("/blackjack/hit")
    public String hit(Model model, HttpSession session) {

        Player player = getPlayer();

        List<String> pCards =
                getSessionCards(session, "blackjackPlayerCards");

        List<String> dCards =
                getSessionCards(session, "blackjackDealerCards");

        Object betObject = session.getAttribute("blackjackBet");

        if (player == null || pCards.isEmpty() || dCards.isEmpty()
                || !(betObject instanceof Integer)) {

            model.addAttribute("player", player);
            model.addAttribute("error", "ゲーム情報がありません。もう一度ゲームを開始してください。");
            model.addAttribute("playing", false);
            clearGame(session);
            return "blackjack";
        }

        int bet = (Integer) betObject;

        // プレイヤーにカードを追加
        pCards.add(drawCard());

        session.setAttribute("blackjackPlayerCards", pCards);

        // 21以上なら勝敗判定
        if (score(pCards) >= 21) {
            return finish(player, pCards, dCards, bet, model, session);
        }

        showGame(player, pCards, dCards, bet, model);

        return "blackjack";
    }

    // STAND：ディーラーと勝負
    @PostMapping("/blackjack/stand")
    public String stand(Model model, HttpSession session) {

        Player player = getPlayer();

        List<String> pCards =
                getSessionCards(session, "blackjackPlayerCards");

        List<String> dCards =
                getSessionCards(session, "blackjackDealerCards");

        Object betObject = session.getAttribute("blackjackBet");

        if (player == null || pCards.isEmpty() || dCards.isEmpty()
                || !(betObject instanceof Integer)) {

            model.addAttribute("player", player);
            model.addAttribute("error", "ゲーム情報がありません。もう一度ゲームを開始してください。");
            model.addAttribute("playing", false);
            clearGame(session);
            return "blackjack";
        }

        int bet = (Integer) betObject;

        // ディーラーは17以上になるまでカードを引く
        while (score(dCards) < 17) {
            dCards.add(drawCard());
        }

        return finish(player, pCards, dCards, bet, model, session);
    }

    // プレイヤー取得
    private Player getPlayer() {
        return playerRepository.findAll()
                .stream()
                .findFirst()
                .orElse(null);
    }

    // セッションからカード情報を取得
    @SuppressWarnings("unchecked")
    private List<String> getSessionCards(HttpSession session, String key) {

        Object cards = session.getAttribute(key);

        if (cards instanceof List<?>) {
            return new ArrayList<>((List<String>) cards);
        }

        return new ArrayList<>();
    }

    // ゲーム情報を削除
    private void clearGame(HttpSession session) {
        session.removeAttribute("blackjackPlayerCards");
        session.removeAttribute("blackjackDealerCards");
        session.removeAttribute("blackjackBet");
    }

    // カードを1枚引く
    private String drawCard() {

        String[] ranks = {
                "A", "2", "3", "4", "5", "6", "7",
                "8", "9", "10", "J", "Q", "K"
        };

        return ranks[random.nextInt(ranks.length)];
    }

    // カードの数値
    private int cardValue(String card) {

        if (card.equals("A")) {
            return 1;
        }

        if (card.equals("J") || card.equals("Q") || card.equals("K")) {
            return 10;
        }

        return Integer.parseInt(card);
    }

    // 合計点数
    private int score(List<String> cards) {

        int total = 0;
        int aces = 0;

        for (String card : cards) {

            total += cardValue(card);

            if (card.equals("A")) {
                aces++;
            }
        }

        // Aは21を超えない場合、11として数える
        while (aces > 0 && total + 10 <= 21) {
            total += 10;
            aces--;
        }

        return total;
    }

    // ゲーム中の画面表示
    private void showGame(
            Player player,
            List<String> pCards,
            List<String> dCards,
            int bet,
            Model model) {

        model.addAttribute("player", player);

        // プレイヤーのカードはすべて表示
        model.addAttribute("playerCards", pCards);
        model.addAttribute("playerScore", score(pCards));

        // ディーラーのカードは1枚目だけ表示
        List<String> dealerDisplayCards = new ArrayList<>();
        dealerDisplayCards.add(dCards.get(0));
        dealerDisplayCards.add("❓");

        model.addAttribute("dealerDisplayCards", dealerDisplayCards);

        // ディーラーの本当の手札・点数はHTMLに渡さない
        model.addAttribute("bet", bet);
        model.addAttribute("playing", true);
    }

    // 勝敗判定・ゲーム終了
    private String finish(
            Player player,
            List<String> pCards,
            List<String> dCards,
            int bet,
            Model model,
            HttpSession session) {

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

        // 払い戻し
        player.setCoin(player.getCoin() + payout);
        playerRepository.save(player);

        // 結果画面にカードをすべて表示
        model.addAttribute("player", player);
        model.addAttribute("playerCards", pCards);
        model.addAttribute("dealerDisplayCards", dCards);

        model.addAttribute("playerScore", pScore);
        model.addAttribute("dealerScore", dScore);

        model.addAttribute("bet", bet);
        model.addAttribute("payout", payout);
        model.addAttribute("result", result);

        model.addAttribute("playing", false);

        // ゲーム終了後はセッション情報を削除
        clearGame(session);

        return "blackjack";
    }
}