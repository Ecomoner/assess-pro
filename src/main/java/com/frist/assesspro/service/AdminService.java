package com.frist.assesspro.service;

import com.frist.assesspro.dto.admin.AppStatisticsDTO;
import com.frist.assesspro.dto.admin.UserManagementDTO;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
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
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final PasswordEncoder passwordEncoder;

    // ============= УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ =============

    /**
     * Получение всех пользователей с пагинацией и фильтрацией
     */
    @Transactional(readOnly = true)
    public Page<UserManagementDTO> getAllUsers(String role, String search, Boolean active, Pageable pageable) {

        // 🔥 ПОДРОБНОЕ ЛОГИРОВАНИЕ
        log.info("========== ПОИСК ПОЛЬЗОВАТЕЛЕЙ ==========");
        log.info("role: '{}'", role);
        log.info("search: '{}'", search);
        log.info("active: {}", active);
        log.info("page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<User> usersPage = userRepository.findUsersWithFilters(role, active, search, pageable);

        log.info("Найдено пользователей: {}", usersPage.getTotalElements());
        log.info("==========================================");

        List<UserManagementDTO> dtos = usersPage.getContent().stream()
                .map(this::convertToUserManagementDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, usersPage.getTotalElements());
    }

    /**
     * Получение пользователя по ID
     */
    @Transactional(readOnly = true)
    public Optional<UserManagementDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToUserManagementDTO);
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
        user.setRole(dto.getRole());
        user.setFirstName(dto.getFirstName().trim());
        user.setLastName(dto.getLastName().trim());
        user.setMiddleName(dto.getMiddleName() != null ? dto.getMiddleName().trim() : null);
        user.setIsProfileComplete(true); // При создании админом - сразу полный профиль
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
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getIsActive() != null) {
            user.setIsActive(dto.getIsActive());
        }
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("Пользователь {} обновлен", updatedUser.getUsername());

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

        // 1. Статистика пользователей
        stats.setTotalUsers(userRepository.countAllUsers());
        stats.setTotalAdmins(userRepository.countByRole(User.Roles.ADMIN));
        stats.setTotalCreators(userRepository.countByRole(User.Roles.CREATOR));
        stats.setTotalTesters(userRepository.countByRole(User.Roles.TESTER));

        List<User> incompleteProfiles = userRepository.findByProfileNotComplete();
        stats.setUsersWithIncompleteProfile((long) incompleteProfiles.size());

        stats.setActiveUsers(userRepository.countByIsActive(true));
        stats.setInactiveUsers(userRepository.countByIsActive(false));

        // 2. Статистика тестов
        stats.setTotalTests(testRepository.countAllTests());
        // 🔥 ИСПРАВЛЕНО: используем правильные методы
        stats.setPublishedTests(testRepository.countByIsPublished(true));
        stats.setDraftTests(testRepository.countByIsPublished(false));
        stats.setTotalQuestions(questionRepository.count());
        stats.setTotalCategories(categoryRepository.count());

        // 3. Статистика прохождений
        List<TestAttempt> allAttempts = testAttemptRepository.findAll();
        stats.setTotalAttempts((long) allAttempts.size());

        long completed = allAttempts.stream()
                .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.COMPLETED)
                .count();
        stats.setCompletedAttempts(completed);

        long inProgress = allAttempts.stream()
                .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.IN_PROGRESS)
                .count();
        stats.setInProgressAttempts(inProgress);

        Double avgScore = allAttempts.stream()
                .filter(a -> a.getStatus() == TestAttempt.AttemptStatus.COMPLETED)
                .mapToInt(a -> a.getTotalScore() != null ? a.getTotalScore() : 0)
                .average()
                .orElse(0.0);
        stats.setAverageScore(avgScore);

        long totalMinutes = allAttempts.stream()
                .filter(a -> a.getStartTime() != null && a.getEndTime() != null)
                .mapToLong(a -> java.time.Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                .sum();
        stats.setTotalTimeSpentMinutes(totalMinutes);

        // 4. Регистрации по дням (последние 30 дней)
        Map<LocalDate, Long> registrations = new LinkedHashMap<>();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // 🔥 ИСПРАВЛЕНО: используем правильный метод
        userRepository.findByCreatedAtAfter(thirtyDaysAgo).stream()
                .collect(Collectors.groupingBy(
                        u -> u.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> registrations.put(e.getKey(), e.getValue()));

        stats.setRegistrationsByDay(registrations);

        // 5. Топ создателей
        List<UserManagementDTO> topCreators = userRepository.findByRole(User.Roles.CREATOR).stream()
                .map(creator -> {
                    UserManagementDTO dto = convertToUserManagementDTO(creator);
                    // 🔥 ИСПРАВЛЕНО: используем правильный метод
                    dto.setTestsCreated(testRepository.countByCreatedBy(creator));
                    return dto;
                })
                .sorted((a, b) -> Long.compare(b.getTestsCreated(), a.getTestsCreated()))
                .limit(10)
                .collect(Collectors.toList());
        stats.setTopCreators(topCreators);

        // 6. Топ тестировщиков
        List<UserManagementDTO> topTesters = userRepository.findByRole(User.Roles.TESTER).stream()
                .map(tester -> {
                    UserManagementDTO dto = convertToUserManagementDTO(tester);
                    dto.setTestsPassed(testAttemptRepository.countByUserIdAndStatus(
                            tester.getId(), TestAttempt.AttemptStatus.COMPLETED));

                    Double avg = testAttemptRepository.findAverageScoreByUserId(tester.getId());
                    dto.setAverageScore(avg != null ? avg : 0.0);
                    return dto;
                })
                .sorted((a, b) -> Long.compare(b.getTestsPassed(), a.getTestsPassed()))
                .limit(10)
                .collect(Collectors.toList());
        stats.setTopTesters(topTesters);

        // 7. Лучшие тестировщики по среднему баллу
        List<UserManagementDTO> bestTesters = userRepository.findByRole(User.Roles.TESTER).stream()
                .map(tester -> {
                    UserManagementDTO dto = convertToUserManagementDTO(tester);
                    Double avg = testAttemptRepository.findAverageScoreByUserId(tester.getId());
                    dto.setAverageScore(avg != null ? avg : 0.0);
                    return dto;
                })
                .sorted((a, b) -> Double.compare(b.getAverageScore(), a.getAverageScore()))
                .limit(10)
                .collect(Collectors.toList());
        stats.setBestTesters(bestTesters);

        // 8. Тесты по категориям
        Map<String, Long> testsByCategory = new LinkedHashMap<>();
        // Здесь можно добавить запрос для получения статистики по категориям
        stats.setTestsByCategory(testsByCategory);

        return stats;
    }

    /**
     * Конвертация User в UserManagementDTO
     */
    private UserManagementDTO convertToUserManagementDTO(User user) {
        UserManagementDTO dto = UserManagementDTO.fromEntity(user);

        // Дополнительная статистика в зависимости от роли
        if (User.Roles.CREATOR.equals(user.getRole())) {
            // 🔥 ИСПРАВЛЕНО: используем правильный метод
            dto.setTestsCreated(testRepository.countByCreatedBy(user));
        } else if (User.Roles.TESTER.equals(user.getRole())) {
            dto.setTestsPassed(testAttemptRepository.countByUserIdAndStatus(
                    user.getId(), TestAttempt.AttemptStatus.COMPLETED));

            Double avg = testAttemptRepository.findAverageScoreByUserId(user.getId());
            dto.setAverageScore(avg != null ? avg : 0.0);
        }

        return dto;
    }
}