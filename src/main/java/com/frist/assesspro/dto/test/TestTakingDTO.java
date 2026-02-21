package com.frist.assesspro.dto.test;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestTakingDTO {
    private Long attemptId;
    private Long testId;
    private String testTitle;
    private Integer timeLimitMinutes;
    private List<QuestionForTakingDTO> questions = new ArrayList<>();
    private Integer currentQuestionIndex = 0;
    private Integer totalQuestions;

    // Новые поля для отслеживания прогресса
    private Integer answeredQuestions = 0;
    private Integer remainingQuestions = 0;

    public boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }

    public Integer getProgressPercentage() {
        if (totalQuestions == null || totalQuestions == 0) {
            return 0;
        }
        return (answeredQuestions * 100) / totalQuestions;
    }
}