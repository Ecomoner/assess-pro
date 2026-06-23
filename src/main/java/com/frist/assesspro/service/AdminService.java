package com.frist.assesspro.service;

import com.frist.assesspro.dto.admin.AppStatisticsCountsDTO;
import com.frist.assesspro.dto.admin.AppStatisticsDTO;
import com.frist.assesspro.dto.admin.UserManagementDTO;
import com.frist.assesspro.entity.Project;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.mapper.UserMapper;
import com.frist.assesspro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository;
    private final UserMapper  userMapper;

    // ============= УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ =============

    /**
     * Получение всех пользователей с пагинацией и фильтрацией
     */
    @Transactional(readOnly = true)
    public Page<UserManagementDTO> getAllUsers(String role, String search, Boolean active, Pageable pageable) {


        log.info("========== ПОИСК ПОЛЬЗОВАТЕЛЕЙ ==========");
        log.info("role: '{}'", role);
        log.info("search: '{}'", search);
        log.info("active: {}", active);
        log.info("page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<User> usersPage = userRepository.findUsersWithFilters(role, active, search, pageable);

        log.info("Найдено пользователей: {}", usersPage.getTotalElements());
        log.info("==========================================");

        Page<UserManagementDTO> dtoPage = usersPage.map(userMapper::toDto);

        return dtoPage;
    }

    /**
     * Получение пользователя по ID
     */
    @Transactional(readOnly = true)
    public Optional<UserManagementDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    /**
     * Создание пользователя (только ADMIN)
     */
    @Transactional
    public User createUser(UserManagementDTO dto, String adminUsername) {
        log.info("Администратор {} создает нового пользователя: {}", adminUsername, dto.getUsername());

        // Проверка уникальности
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Имя пользователя уже занято");
        }


        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        } else {
            throw new RuntimeException("Пароль обязателен");
        }
        user.setRole(dto.getRole());
        if (User.Roles.TESTER.equals(dto.getRole())) {
            if (dto.getProjectId() == null) {
                throw new IllegalArgumentException("Для тестировщика необходимо выбрать проект");
            }
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));
            user.setProject(project);
        } else {
            user.setProject(null);
        }
        user.setFirstName(dto.getFirstName().trim());
        user.setLastName(dto.getLastName().trim());
        user.setMiddleName(dto.getMiddleName() != null ? dto.getMiddleName().trim() : null);
        user.setIsProfileComplete(true);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("Пользователь {} создан с ID: {}", savedUser.getUsername(), savedUser.getId());

        return savedUser;
    }

    /**
     * Обновление пользователя
     */
    @Transactional
    public User updateUser(Long id, UserManagementDTO dto, String adminUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        log.info("Администратор {} обновляет пользователя: {}", adminUsername, user.getUsername());

        // Обновляем только разрешенные поля
        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName().trim());
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName().trim());
        }
        if (dto.getMiddleName() != null) {
            user.setMiddleName(dto.getMiddleName().trim());
        }
        user.setIsProfileComplete(true);
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getIsActive() != null) {
            user.setIsActive(dto.getIsActive());
        }
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (User.Roles.TESTER.equals(dto.getRole()) && dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));
            user.setProject(project);
        }else {
            user.setProject(null);
        }
        User updatedUser = userRepository.save(user);
        log.info("Пользователь {} обновлён, проект: {}",
                updatedUser.getUsername(),
                updatedUser.getProject() != null ? updatedUser.getProject().getName() : "нет");

        return updatedUser;
    }

    /**
     * (Де)активация пользователя
     */
    @Transactional
    public User toggleUserStatus(Long id, boolean active, String adminUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Нельзя деактивировать самого себя
        if (user.getUsername().equals(adminUsername)) {
            throw new IllegalArgumentException("Нельзя деактивировать собственную учетную запись");
        }

        user.setIsActive(active);
        User updatedUser = userRepository.save(user);

        log.info("Администратор {} {} пользователя {}",
                adminUsername, active ? "активировал" : "деактивировал", user.getUsername());

        return updatedUser;
    }

    /**
     * Удаление пользователя
     */
    @Transactional
    public void deleteUser(Long id, String adminUsername) {
        if (id == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Нельзя удалить самого себя
        if (user.getUsername().equals(adminUsername)) {
            throw new IllegalArgumentException("Нельзя удалить собственную учетную запись");
        }

        log.info("Администратор {} удаляет пользователя: {}", adminUsername, user.getUsername());
        userRepository.delete(user);
    }

    // ============= СТАТИСТИКА ПРИЛОЖЕНИЯ =============

    /**
     * Получение общей статистики приложения
     */
    @Transactional(readOnly = true)
    public AppStatisticsDTO getAppStatistics() {
        AppStatisticsDTO stats = new AppStatisticsDTO();
        AppStatisticsCountsDTO counts = userRepository.getAppStatisticsCounts();

        stats.setTotalUsers(counts.getTotalUsers());
        stats.setTotalAdmins(counts.getTotalAdmins());
        stats.setTotalCreators(counts.getTotalCreators());
        stats.setTotalTesters(counts.getTotalTesters());
        stats.setUsersWithIncompleteProfile(counts.getIncompleteProfiles());
        stats.setActiveUsers(counts.getActiveUsers());
        stats.setInactiveUsers(counts.getInactiveUsers());

        stats.setTotalTests(counts.getTotalTests());
        stats.setPublishedTests(counts.getPublishedTests());
        stats.setDraftTests(counts.getDraftTests());
        stats.setTotalQuestions(counts.getTotalQuestions());
        stats.setTotalCategories(counts.getTotalCategories());

        stats.setTotalAttempts(counts.getTotalAttempts());
        stats.setCompletedAttempts(counts.getCompletedAttempts());
        stats.setInProgressAttempts(counts.getInProgressAttempts());

        // ============= 3. СТАТИСТИКА ПРОХОЖДЕНИЙ =============
        List<Object[]> avgAndMinutes = testAttemptRepository.findAverageScoreAndTotalMinutes();
        double avgScore = 0.0;
        long totalMinutes = 0L;
        if (!avgAndMinutes.isEmpty()) {
            Object[] row = avgAndMinutes.get(0);
            avgScore = ((Number) row[0]).doubleValue();
            totalMinutes = ((Number) row[1]).longValue();
        }

        stats.setAverageScore(avgScore);
        stats.setTotalTimeSpentMinutes(totalMinutes);

        // ============= 4. ДИНАМИКА =============

        // Регистрации по дням (последние 30 дней)
        Map<LocalDate, Long> registrations = new LinkedHashMap<>();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        userRepository.findByCreatedAtAfter(thirtyDaysAgo).stream()
                .collect(Collectors.groupingBy(
                        u -> u.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> registrations.put(e.getKey(), e.getValue()));
        stats.setRegistrationsByDay(registrations);

        // Прохождения по дням (последние 30 дней)

        // Тесты по категориям
        Map<String, Long> testsByCategory = new LinkedHashMap<>();
        List<Object[]> categoryStats = testRepository.countTestsByCategory();
        for (Object[] stat : categoryStats) {
            if (stat.length >= 2 && stat[0] != null) {
                String categoryName = (String) stat[0];
                Long count = ((Number) stat[1]).longValue();
                testsByCategory.put(categoryName, count);
            }
        }
        stats.setTestsByCategory(testsByCategory);

        // Средний балл по категориям
        Map<String, Double> avgScoreByCategory = new LinkedHashMap<>();
        List<Object[]> avgStats = testRepository.averageScoreByCategory();
        for (Object[] stat : avgStats) {
            if (stat.length >= 2 && stat[0] != null) {
                String categoryName = (String) stat[0];
                Double avgrScore = ((Number) stat[1]).doubleValue();
                avgScoreByCategory.put(categoryName, avgrScore);
            }
        }
        stats.setAverageScoreByCategory(avgScoreByCategory);

        // ============= 5. ТОПЫ =============

        // Топ создателей (по количеству тестов)
        List<UserManagementDTO> topCreators = userRepository.findByRole(User.Roles.CREATOR).stream()
                .map(creator -> {
                    UserManagementDTO dto = UserManagementDTO.fromEntity(creator);
                    dto.setTestsCreated(testRepository.countByCreatedBy(creator));
                    return dto;
                })
                .sorted((a, b) -> Long.compare(b.getTestsCreated(), a.getTestsCreated()))
                .limit(10)
                .collect(Collectors.toList());
        stats.setTopCreators(topCreators);

        // Топ тестировщиков (по количеству прохождений) и лучшие по среднему баллу
        List<User> allTesters = userRepository.findByRole(User.Roles.TESTER);
        List<UserManagementDTO> topTesters = new ArrayList<>();
        List<UserManagementDTO> bestTesters = new ArrayList<>();

        for (User tester : allTesters) {
            UserManagementDTO dto = UserManagementDTO.fromEntity(tester);

            // Статистика тестировщика
            long completedAttempts = testAttemptRepository.countByUserIdAndStatus(
                    tester.getId(), TestAttempt.AttemptStatus.COMPLETED);
            Double avgScoreForTester = testAttemptRepository.findAverageScoreByUserId(tester.getId());

            dto.setTestsPassed(completedAttempts);
            dto.setAverageScore(avgScoreForTester != null ? avgScoreForTester : 0.0);

            topTesters.add(dto);
            bestTesters.add(dto);
        }

        // Сортируем и ограничиваем топы
        stats.setTopTesters(
                topTesters.stream()
                        .sorted((a, b) -> Long.compare(b.getTestsPassed(), a.getTestsPassed()))
                        .limit(10)
                        .collect(Collectors.toList())
        );

        stats.setBestTesters(
                bestTesters.stream()
                        .sorted((a, b) -> Double.compare(b.getAverageScore(), a.getAverageScore()))
                        .limit(10)
                        .collect(Collectors.toList())
        );

        return stats;
    }

}