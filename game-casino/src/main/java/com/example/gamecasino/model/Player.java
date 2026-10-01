package com.example.gamecasino.model;

public class Player {

    private String playerName;
    private int coin;
    private int debt;

    public Player() {
        this.coin = 10000;
        this.debt = 0;
    }

    public Player(String playerName) {
        this.playerName = playerName;
        this.coin = 10000;
        this.debt = 0;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getCoin() {
        return coin;
    }

    public void setCoin(int coin) {
        this.coin = coin;
    }

    public int getDebt() {
        return debt;
    }

    public void setDebt(int debt) {
        this.debt = debt;
    }

    public int getBalance() {
        return coin - debt;
    }
}