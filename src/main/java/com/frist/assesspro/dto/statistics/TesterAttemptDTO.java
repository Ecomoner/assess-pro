package com.frist.assesspro.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TesterAttemptDTO {
    private Long attemptId;
    private String testerUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer score;
    private Integer maxScore;
    private Double percentage;
    private Long durationMinutes;
    private Long testId;

    /**
     * Получает форматированную строку длительности
     */
    public String getFormattedDuration() {
        if (durationMinutes == null || durationMinutes == 0) {
            return "0 мин";
        }

        long minutes = durationMinutes;
        if (minutes < 60) {
            return minutes + " мин";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + " ч";
            } else {
                return hours + " ч " + remainingMinutes + " мин";
            }
        }
    }

    /**
     * Проверяет, завершена ли попытка
     */
    public boolean isCompleted() {
        return endTime != null;
    }

    /**
     * Получает статус попытки
     */
    public String getStatus() {
        if (!isCompleted()) {
            return "В процессе";
        } else if (percentage >= 70) {
            return "Успешно";
        } else if (percentage >= 50) {
            return "Средне";
        } else {
            return "Неудачно";
        }
    }

    /**
     * Получает CSS класс для статуса
     */
    public String getStatusClass() {
        if (!isCompleted()) {
            return "bg-warning";
        } else if (percentage >= 70) {
            return "bg-success";
        } else if (percentage >= 50) {
            return "bg-info";
        } else {
            return "bg-danger";
        }
    }

    /**
     * Получает цвет для процента
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

    /**
     * Получает CSS класс для границы строки
     */
    public String getBorderClass() {
        if (!isCompleted()) {
            return "border-secondary";
        }

        if (percentage >= 70) {
            return "border-success";
        } else if (percentage >= 50) {
            return "border-warning";
        } else {
            return "border-danger";
        }
    }

    /**
     * Получает CSS класс для бейджа в зависимости от процента
     */
    public String getPercentageBadgeClass() {
        if (percentage == null) {
            return "bg-secondary";
        }

        if (percentage >= 80) return "bg-success";
        if (percentage >= 60) return "bg-info";
        if (percentage >= 40) return "bg-warning";
        return "bg-danger";
    }

}