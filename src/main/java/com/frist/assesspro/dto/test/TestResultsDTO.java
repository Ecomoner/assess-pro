package com.frist.assesspro.dto.test;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TestResultsDTO {

    private Long attemptId;
    private Long testId;
    private String testTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalScore;
    private Integer maxPossibleScore;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer correctAnswers;
    private List<QuestionResultDTO> questionResults;

    public Double getPercentage() {
        if (maxPossibleScore == null || maxPossibleScore == 0) {
            return 0.0;
        }
        return (totalScore.doubleValue() / maxPossibleScore.doubleValue()) * 100;
    }

    public String getGrade() {
        double percentage = getPercentage();
        if (percentage >= 90) return "Отлично (A)";
        if (percentage >= 80) return "Хорошо (B)";
        if (percentage >= 70) return "Удовлетворительно (C)";
        if (percentage >= 60) return "Достаточно (D)";
        return "Неудовлетворительно (F)";
    }

}
