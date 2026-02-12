package com.frist.assesspro.dto;

import lombok.Data;

@Data
public class UserStatisticsDTO {
    private long totalAttempts;
    private long completedAttempts;
    private long inProgressAttempts;
    private double averageScore;
    private double bestScore;
}
