package com.frist.assesspro.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TesterDetailedAnswersDTO {
    private Long attemptId;
    private String testerUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<QuestionAnswerDetailDTO> questionAnswers;
    private TestSummaryDTO summary;

    /**
     * Получает форматированную длительность
     */
    public String getFormattedDuration() {
        if (startTime == null || endTime == null) {
            return "Нет данных";
        }

        try {
            Duration duration = Duration.between(startTime, endTime);
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;

            if (hours > 0) {
                return String.format("%d ч %d мин", hours, minutes);
            } else if (minutes > 0) {
                return String.format("%d мин %d сек", minutes, seconds);
            } else {
                return String.format("%d сек", seconds);
            }
        } catch (Exception e) {
            return "Ошибка вычисления";
        }
    }

    /**
     * Получает длительность в минутах
     */
    public Long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0L;
        }

        try {
            return Duration.between(startTime, endTime).toMinutes();
        } catch (Exception e) {
            return 0L;
        }
    }
}
