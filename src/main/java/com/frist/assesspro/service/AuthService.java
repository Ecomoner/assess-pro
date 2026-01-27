package com.frist.assesspro.service;

import com.frist.assesspro.dto.RegistrationDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(RegistrationDTO registrationDTO) {
        log.info("=== НАЧАЛО РЕГИСТРАЦИИ ===");
        log.info("Username: {}", registrationDTO.getUsername());
        log.info("Role: {}", registrationDTO.getRole());
        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            throw new IllegalArgumentException("Имя пользователя уже занято");
        }

        // Проверка совпадения паролей
        if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }

        // Создание пользователя
        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setRole(registrationDTO.getRole());
        user.setCreatedAt(LocalDateTime.now());
        user.setIsActive(true);

        return userRepository.save(user);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

}
