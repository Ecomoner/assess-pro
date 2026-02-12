package com.frist.assesspro.dto;

import lombok.Data;

@Data
public class DashboardStatsDTO {

    private Long totalTests;
    private Long publishedTests;
    private Long totalQuestions;
    // Для тестировщика
    private Long totalAttempts;
    private Long completedTests;
    private Long inProgressTests;
    private Integer averageScore;
    private Integer bestScore;
    private Long availableTests;
    private Long totalMinutes;
    // Для статистики
    private Long uniqueTesters;
    private Long testsWithAttempts;
    private Integer recentActivityDays;
    private Long totalCategories;
}