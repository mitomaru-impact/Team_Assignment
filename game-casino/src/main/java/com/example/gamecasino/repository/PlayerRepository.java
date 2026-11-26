package com.example.gamecasino.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gamecasino.model.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // 最後に登録したプレイヤーを取得
    java.util.Optional<Player> findTopByOrderByPlayerIdDesc();

}