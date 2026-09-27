package com.reme.re_me.service;

import com.reme.re_me.entity.User;
import com.reme.re_me.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PublicUserIdMigration implements ApplicationRunner {

    private final UserRepository userRepository;

    public PublicUserIdMigration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        var usersWithoutPublicId = userRepository.findAllByPublicIdIsNull();
        for (User user : usersWithoutPublicId) {
            user.setPublicId(UUID.randomUUID().toString());
        }
        userRepository.saveAllAndFlush(usersWithoutPublicId);
    }
}
