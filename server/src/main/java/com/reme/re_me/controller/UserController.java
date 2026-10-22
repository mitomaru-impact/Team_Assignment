package com.reme.re_me.controller;

import com.reme.re_me.dto.UserDisplayNameDto;
import com.reme.re_me.dto.OnlineStatusDto;
import com.reme.re_me.dto.UpdateOnlineStatusRequest;
import com.reme.re_me.entity.User;
import com.reme.re_me.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.reme.re_me.service.PublicUserIdService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PublicUserIdService publicUserIdService;

    public UserController(
            UserRepository userRepository,
            PublicUserIdService publicUserIdService) {
        this.userRepository = userRepository;
        this.publicUserIdService = publicUserIdService;
    }

    @GetMapping("/display-name")
    public ResponseEntity<UserDisplayNameDto> getDisplayName(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(User::getName)
                .map(UserDisplayNameDto::new)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/online-status")
    public ResponseEntity<?> getOnlineStatus(@RequestParam String userId) {
        try {
            User user = publicUserIdService.resolve(userId);
            return ResponseEntity.ok(new OnlineStatusDto(user.isOnlineStatusVisible()));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @org.springframework.web.bind.annotation.PutMapping("/online-status")
    public ResponseEntity<?> updateOnlineStatus(
            @org.springframework.web.bind.annotation.RequestBody UpdateOnlineStatusRequest request) {
        try {
            User user = publicUserIdService.resolve(request.userId());
            user.setOnlineStatusVisible(request.visible());
            userRepository.save(user);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }
}
