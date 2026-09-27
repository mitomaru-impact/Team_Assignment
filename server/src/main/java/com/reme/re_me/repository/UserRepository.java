package com.reme.re_me.repository;

import com.reme.re_me.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPublicId(String publicId);
    List<User> findAllByPublicIdIsNull();
    boolean existsByEmail(String email);
}