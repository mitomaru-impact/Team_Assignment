package com.reme.re_me.controller;

import com.reme.re_me.dto.UserDisplayNameDto;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/display-name")
    public ResponseEntity<UserDisplayNameDto> getDisplayName(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(User::getName)
                .map(UserDisplayNameDto::new)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
