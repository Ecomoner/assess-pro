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
        log.info("Начало регистрации пользователя: {}", registrationDTO.getUsername());

        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            log.error("Имя пользователя уже занято: {}", registrationDTO.getUsername());
            throw new IllegalArgumentException("Имя пользователя уже занято");
        }

        // Проверка совпадения паролей
        if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
            log.error("Пароли не совпадают для пользователя: {}", registrationDTO.getUsername());
            throw new IllegalArgumentException("Пароли не совпадают");
        }

        if (!User.Roles.isValidRole(registrationDTO.getRole())) {
            log.error("Некорректная роль: {}", registrationDTO.getRole());
            throw new IllegalArgumentException("Некорректная роль");
        }

        // Создание пользователя
        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setRole(registrationDTO.getRole());
        user.setCreatedAt(LocalDateTime.now());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        log.info("Пользователь успешно зарегистрирован: {} (ID: {})",
                savedUser.getUsername(), savedUser.getId());

        return savedUser;
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

}
