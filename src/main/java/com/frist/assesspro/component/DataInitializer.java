package com.frist.assesspro.component;

import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Инициализация начальных данных...");

        // Создаем администратора, если его нет
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.Roles.CREATOR);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setIsActive(true);

            userRepository.save(admin);
            log.info("Создан администратор: admin / admin123");
        }

        // Создаем тестового пользователя, если его нет
        if (!userRepository.existsByUsername("tester")) {
            User tester = new User();
            tester.setUsername("tester");
            tester.setPassword(passwordEncoder.encode("tester123"));
            tester.setRole(User.Roles.TESTER);
            tester.setCreatedAt(LocalDateTime.now());
            tester.setIsActive(true);

            userRepository.save(tester);
            log.info("Создан тестировщик: tester / tester123");
        }

        // Проверяем количество пользователей
        long userCount = userRepository.count();
        long creatorCount = userRepository.countByRole(User.Roles.CREATOR);
        long testerCount = userRepository.countByRole(User.Roles.TESTER);

        log.info("Инициализация завершена. Пользователей в базе: {}", userCount);
        log.info("Создателей: {}, Тестировщиков: {}", creatorCount, testerCount);
        log.info("Для входа используйте:");
        log.info("1. admin / admin123 (роль: CREATOR)");
        log.info("2. tester / tester123 (роль: TESTER)");
    }
}
