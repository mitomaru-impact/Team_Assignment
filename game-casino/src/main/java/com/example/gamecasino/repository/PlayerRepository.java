package com.example.gamecasino.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gamecasino.model.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}