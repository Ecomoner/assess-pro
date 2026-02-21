package com.frist.assesspro.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TesterStatisticsDTO {
    private Long attemptId;
    private String testerUsername;
    private String testerFullName;
    private boolean profileComplete;
    private String cooldownStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer score;
    private Integer maxScore;
    private Double percentage;
    private Long durationMinutes;
    private Long testId;
    private String testTitle;

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

    public String getStatusBadgeClass() {
        if ("Доступен".equals(cooldownStatus)) {
            return "bg-success";
        } else if ("Ограничение".equals(cooldownStatus)) {
            return "bg-warning";
        } else if ("Исключение".equals(cooldownStatus)) {
            return "bg-info";
        }
        return "bg-secondary";
    }

    public String getScoreClass() {
        if (percentage == null) return "text-secondary";
        if (percentage >= 80) return "text-success";
        if (percentage >= 60) return "text-info";
        if (percentage >= 40) return "text-warning";
        return "text-danger";
    }
}
