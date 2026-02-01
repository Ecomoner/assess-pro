package com.frist.assesspro.dto.test;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestHistoryDTO {
    private Long attemptId;
    private Long testId;
    private String testTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer totalScore;
    private Integer maxPossibleScore;

    public Double getPercentage() {
        if (maxPossibleScore == null || maxPossibleScore == 0) {
            return 0.0;
        }
        return (totalScore.doubleValue() / maxPossibleScore.doubleValue()) * 100;
    }
}
