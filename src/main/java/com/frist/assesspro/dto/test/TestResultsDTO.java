package com.frist.assesspro.dto.test;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    public Long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    public String getFormattedDuration() {
        Long minutes = getDurationMinutes();
        if (minutes == null || minutes == 0) {
            return "0 минут";
        }

        if (minutes < 60) {
            return minutes + " минут";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + " час" + (hours > 1 ? "а" : "");
            } else {
                return hours + " час" + (hours > 1 ? "а" : "") + " " + remainingMinutes + " минут";
            }
        }
    }

}
