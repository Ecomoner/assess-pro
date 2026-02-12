package com.frist.assesspro.service;

import com.frist.assesspro.dto.RegistrationDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
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

    @CacheEvict(value = {"creatorStats", "testerStats"}, allEntries = true)
    @Transactional
    public User registerUser(RegistrationDTO registrationDTO) {
        log.info("Начало регистрации пользователя: {}", registrationDTO.getUsername());

        // Дополнительная валидация
        if (registrationDTO.getUsername().trim().length() < 3) {
            throw new IllegalArgumentException("Имя пользователя слишком короткое");
        }

        if (registrationDTO.getPassword().contains(registrationDTO.getUsername())) {
            throw new IllegalArgumentException("Пароль не должен содержать имя пользователя");
        }

        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            throw new IllegalArgumentException("Имя пользователя уже занято");
        }

        if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }

        // Проверка сложности пароля
        if (!isPasswordStrong(registrationDTO.getPassword())) {
            throw new IllegalArgumentException(
                    "Пароль недостаточно надежен. Используйте буквы, цифры и специальные символы");
        }

        if (!User.Roles.isValidRole(registrationDTO.getRole())) {
            throw new IllegalArgumentException("Некорректная роль");
        }

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

    private boolean isPasswordStrong(String password) {
        // Минимум 6 символов, хотя бы одна цифра и одна буква
        return password.length() >= 6 &&
                password.matches(".*\\d.*") &&
                password.matches(".*[a-zA-Z].*");
    }

}
