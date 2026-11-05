package com.example.gamecasino.game;
 
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
 
public class YachtGame {
 
    private final Random random = new Random();
 
    private List<Integer> dice = new ArrayList<>();
    private final Map<String, Integer> scores = new LinkedHashMap<>();
 
    private int rollCount = 0;
 
    private static final String[] CATEGORIES = {
        "aces", "deuces", "threes", "fours",
        "fives", "sixes", "fourKind", "fullHouse",
        "smallStraight", "largeStraight", "chance", "yacht"
    };
 
    public YachtGame() {
        for (String category : CATEGORIES) {
            scores.put(category, null);
        }
    }
 
    public void startTurn() {
        dice.clear();
        for (int i = 0; i < 5; i++) {
            dice.add(random.nextInt(6) + 1);
        }
        rollCount = 1;
    }
 
    public void reroll(List<Integer> keep) {
        if (rollCount >= 3) {
            return;
        }
 
        List<Integer> newDice = new ArrayList<>();
 
        for (int i = 0; i < 5; i++) {
            if (keep != null && keep.contains(i)) {
                newDice.add(dice.get(i));
            } else {
                newDice.add(random.nextInt(6) + 1);
            }
        }
 
        dice = newDice;
        rollCount++;
    }
 
    public int calculateScore(String category) {
        int[] counts = new int[7];
        int sum = 0;
 
        for (int die : dice) {
            counts[die]++;
            sum += die;
        }
 
        switch (category) {
            case "aces":
            case "deuces":
            case "threes":
            case "fours":
            case "fives":
            case "sixes":
                int number = getFaceNumber(category);
                return counts[number] * number;
 
            case "fourKind":
                for (int i = 1; i <= 6; i++) {
                    if (counts[i] >= 4) return sum;
                }
                return 0;
 
            case "fullHouse":
                boolean three = false;
                boolean two = false;
 
                for (int i = 1; i <= 6; i++) {
                    if (counts[i] == 3) three = true;
                    if (counts[i] == 2) two = true;
                }
 
                return three && two ? 25 : 0;
 
            case "smallStraight":
                if (hasStraight(counts, 4)) return 30;
                return 0;
 
            case "largeStraight":
                if (hasLargeStraight(counts)) return 40;
                return 0;
 
            case "chance":
                return sum;
 
            case "yacht":
                for (int i = 1; i <= 6; i++) {
                    if (counts[i] == 5) return 50;
                }
                return 0;
 
            default:
                return 0;
        }
    }
 
    private int getFaceNumber(String category) {
        switch (category) {
            case "aces": return 1;
            case "deuces": return 2;
            case "threes": return 3;
            case "fours": return 4;
            case "fives": return 5;
            case "sixes": return 6;
            default: return 0;
        }
    }
 
    private boolean hasStraight(int[] counts, int length) {
        for (int start = 1; start <= 7 - length; start++) {
            boolean found = true;
 
            for (int i = start; i < start + length; i++) {
                if (counts[i] == 0) {
                    found = false;
                    break;
                }
            }
 
            if (found) return true;
        }
 
        return false;
    }
 
    private boolean hasLargeStraight(int[] counts) {
        boolean first = true;
        boolean second = true;
 
        for (int i = 1; i <= 5; i++) {
            if (counts[i] == 0) first = false;
        }
 
        for (int i = 2; i <= 6; i++) {
            if (counts[i] == 0) second = false;
        }
 
        return first || second;
    }
 
    public void saveScore(String category) {
        if (scores.containsKey(category) && scores.get(category) == null) {
            scores.put(category, calculateScore(category));
        }
    }
 
    public boolean isCategoryAvailable(String category) {
        return scores.containsKey(category) && scores.get(category) == null;
    }
 
    public int getTotalScore() {
        int total = 0;
 
        for (Integer score : scores.values()) {
            if (score != null) {
                total += score;
            }
        }
 
        return total;
    }
 
    public boolean isFinished() {
        return scores.values().stream().noneMatch(score -> score == null);
    }
 
    public List<Integer> getDice() {
        return dice;
    }
 
    public Map<String, Integer> getScores() {
        return scores;
    }
 
    public int getRollCount() {
        return rollCount;
    }
 
    public boolean canRoll() {
        return rollCount < 3;
    }
 
    public String[] getCategories() {
        return CATEGORIES;
    }
}