package com.frist.assesspro.dto.admin;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppStatisticsCountsDTO {

    private long totalUsers;
    private long totalAdmins;
    private long totalCreators;
    private long totalTesters;
    private long incompleteProfiles;
    private long activeUsers;
    private long inactiveUsers;
    private long totalTests;
    private long publishedTests;
    private long draftTests;
    private long totalQuestions;
    private long totalCategories;
    private long totalAttempts;
    private long completedAttempts;
    private long inProgressAttempts;
}
