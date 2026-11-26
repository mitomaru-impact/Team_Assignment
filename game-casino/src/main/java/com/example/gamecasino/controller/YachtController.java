package com.example.gamecasino.controller;
 
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gamecasino.game.YachtGame;
import com.example.gamecasino.model.Player;
import com.example.gamecasino.repository.PlayerRepository;
 
@Controller
public class YachtController {
 
    private final PlayerRepository playerRepository;
 
    private static final String GAME_KEY = "yachtGame";
 
    private final Map<String, String> labels = new LinkedHashMap<>();
 
    public YachtController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
 
        labels.put("aces", "エース（1の目）");
        labels.put("deuces", "デュース（2の目）");
        labels.put("threes", "トレイ（3の目）");
        labels.put("fours", "フォー（4の目）");
        labels.put("fives", "ファイブ（5の目）");
        labels.put("sixes", "シックス（6の目）");
        labels.put("fourKind", "フォーカード");
        labels.put("fullHouse", "フルハウス");
        labels.put("smallStraight", "スモールストレート");
        labels.put("largeStraight", "ラージストレート");
        labels.put("chance", "チャンス");
        labels.put("yacht", "ヨット");
    }
 
    @GetMapping("/yacht")
    public String yacht(Model model, HttpSession session) {
 
        YachtGame game = (YachtGame) session.getAttribute(GAME_KEY);
 
        Player player = getPlayer();
 
        model.addAttribute("player", player);
        model.addAttribute("game", game);
        model.addAttribute("labels", labels);
 
        if (game != null) {
            addGameData(game, model);
        }
 
        return "yacht";
    }
 
    @PostMapping("/yacht/start")
    public String start(HttpSession session) {
 
        YachtGame game = new YachtGame();
        game.startTurn();
 
        session.setAttribute(GAME_KEY, game);
 
        return "redirect:/yacht";
    }
 
    @PostMapping("/yacht/roll")
    public String roll(
            @RequestParam(required = false) List<Integer> keep,
            HttpSession session) {
 
        YachtGame game = getGame(session);
 
        if (game != null && game.canRoll()) {
            game.reroll(keep == null ? new ArrayList<>() : keep);
        }
 
        return "redirect:/yacht";
    }
 
    @PostMapping("/yacht/score")
    public String score(
            @RequestParam String category,
            HttpSession session) {
 
        YachtGame game = getGame(session);
 
        if (game == null || !game.isCategoryAvailable(category)) {
            return "redirect:/yacht";
        }
 
        game.saveScore(category);
 
        if (!game.isFinished()) {
            game.startTurn();
        }
 
        return "redirect:/yacht";
    }
 
    @PostMapping("/yacht/reset")
    public String reset(HttpSession session) {
 
        session.removeAttribute(GAME_KEY);
 
        return "redirect:/yacht";
    }
 
    private YachtGame getGame(HttpSession session) {
        return (YachtGame) session.getAttribute(GAME_KEY);
    }
 
    private Player getPlayer() {
        return playerRepository.findTopByOrderByPlayerIdDesc()
                .orElse(null);
    }
 
    private void addGameData(YachtGame game, Model model) {
 
        model.addAttribute("dice", game.getDice());
        model.addAttribute("rollCount", game.getRollCount());
        model.addAttribute("canRoll", game.canRoll());
        model.addAttribute("scores", game.getScores());
        model.addAttribute("totalScore", game.getTotalScore());
        model.addAttribute("finished", game.isFinished());
 
        Map<String, Integer> possibleScores = new LinkedHashMap<>();
 
        for (String category : game.getCategories()) {
            if (game.isCategoryAvailable(category)) {
                possibleScores.put(
                        category,
                        game.calculateScore(category)
                );
            }
        }
 
        model.addAttribute("possibleScores", possibleScores);
    }
}