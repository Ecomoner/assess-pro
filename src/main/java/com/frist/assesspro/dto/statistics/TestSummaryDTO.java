package com.frist.assesspro.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestSummaryDTO {
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer correctAnswers;
    private Integer totalScore;
    private Double percentage;

    /**
     * Получает процент отвеченных вопросов
     */
    public Double getAnsweredPercentage() {
        if (totalQuestions == null || totalQuestions == 0) {
            return 0.0;
        }
        return (answeredQuestions.doubleValue() / totalQuestions.doubleValue()) * 100;
    }

    /**
     * Получает оценку по 5-балльной системе
     */
    public String getGrade() {
        if (percentage == null) {
            return "Нет данных";
        }

        if (percentage >= 90) return "Отлично (A)";
        if (percentage >= 80) return "Хорошо (B)";
        if (percentage >= 70) return "Удовлетворительно (C)";
        if (percentage >= 60) return "Достаточно (D)";
        return "Неудовлетворительно (F)";
    }

    /**
     * Получает цвет CSS класса для процента
     */
    public String getPercentageClass() {
        if (percentage == null) {
            return "text-secondary";
        }

        if (percentage >= 80) return "text-success";
        if (percentage >= 60) return "text-info";
        if (percentage >= 40) return "text-warning";
        return "text-danger";
    }
}