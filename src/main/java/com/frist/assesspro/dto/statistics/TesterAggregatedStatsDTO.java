package com.frist.assesspro.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TesterAggregatedStatsDTO {
    private String testerUsername;
    private Long testerId;
    private Long totalAttempts;
    private Long completedAttempts;
    private Double averagePercentage;
    private Double bestPercentage;
    private Double worstPercentage;
    private Long totalDurationMinutes;
    private LocalDateTime firstAttemptDate;
    private LocalDateTime lastAttemptDate;
    private List<Long> attemptIds; // Все ID попыток этого тестировщика
    private List<TesterAttemptDTO> recentAttempts; // Последние 3 попытки
}