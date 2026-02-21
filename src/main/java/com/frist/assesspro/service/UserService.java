package com.frist.assesspro.service;

import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Поиск пользователя по username
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        log.debug("Поиск пользователя: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Получение пользователя по username с проверкой существования
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
    }

    /**
     * Проверка существования пользователя
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Получение полного имени пользователя
     */
    @Transactional(readOnly = true)
    public String getUserFullName(String username) {
        return userRepository.findByUsername(username)
                .map(User::getFullName)
                .orElse(username);
    }

    /**
     * Получение всех тестировщиков (для админки)
     */
    @Transactional(readOnly = true)
    public Page<User> findAllTesters(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return userRepository.searchTesters(search.trim(), pageable);
        } else {
            return userRepository.findByRole("ROLE_TESTER", pageable);
        }
    }

    /**
     * Получение всех создателей (для админки)
     */
    @Transactional(readOnly = true)
    public List<User> getAllCreators() {
        return userRepository.findByRole(User.Roles.CREATOR);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getRole().equals(User.Roles.ADMIN))
                .orElse(false);
    }

}