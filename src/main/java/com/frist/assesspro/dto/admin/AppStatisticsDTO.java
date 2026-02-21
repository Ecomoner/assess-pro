package com.frist.assesspro.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppStatisticsDTO {

    // Пользователи
    private Long totalUsers;
    private Long totalAdmins;
    private Long totalCreators;
    private Long totalTesters;
    private Long usersWithIncompleteProfile;
    private Long activeUsers;
    private Long inactiveUsers;

    // Тесты
    private Long totalTests;
    private Long publishedTests;
    private Long draftTests;
    private Long totalQuestions;
    private Long totalCategories;

    // Прохождения
    private Long totalAttempts;
    private Long completedAttempts;
    private Long inProgressAttempts;
    private Double averageScore;
    private Long totalTimeSpentMinutes;

    // Динамика
    private Map<LocalDate, Long> registrationsByDay;
    private Map<LocalDate, Long> attemptsByDay;
    private Map<String, Long> testsByCategory;
    private Map<String, Double> averageScoreByCategory;

    // Топы
    private List<UserManagementDTO> topCreators;        // по количеству тестов
    private List<UserManagementDTO> topTesters;         // по количеству прохождений
    private List<UserManagementDTO> bestTesters;        // по среднему баллу
}
