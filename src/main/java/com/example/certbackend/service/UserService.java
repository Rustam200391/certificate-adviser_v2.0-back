package com.example.certbackend.service;

import com.example.certbackend.entity.User;
import com.example.certbackend.entity.UserRole;
import com.example.certbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String username, String password, UserRole role) {
        if (username == null || username.isBlank() || username.trim().length() > 100) {
            throw new IllegalArgumentException("Username is required and must be at most 100 characters");
        }
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Password must contain at least 12 characters");
        }
        if (role == null) throw new IllegalArgumentException("Role is required");
        String normalized = normalize(username);
        if (users.existsByUsername(normalized)) throw new UsernameAlreadyExistsException();
        User user = new User();
        user.setUsername(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        try {
            return users.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
            throw new UsernameAlreadyExistsException();
        }
    }

    public static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public static class UsernameAlreadyExistsException extends RuntimeException { }
}
