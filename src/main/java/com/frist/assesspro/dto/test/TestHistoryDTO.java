package com.frist.assesspro.dto.test;

import com.frist.assesspro.entity.TestAttempt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestHistoryDTO {
    private Long attemptId;
    private Long testId;
    private String testTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private TestAttempt.AttemptStatus status;
    private Integer totalScore;
    private Long maxPossibleScore;

    public Integer getMaxPossibleScoreInteger() {
        return maxPossibleScore != null ? maxPossibleScore.intValue() : 0;
    }

    public Double getPercentage() {
        if (totalScore == null || maxPossibleScore == null || maxPossibleScore == 0) {
            return 0.0;
        }
        return (totalScore.doubleValue() / maxPossibleScore.doubleValue()) * 100;
    }

    public Long getElapsedMinutes() {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    public Long getDurationMinutes() {
        return getElapsedMinutes();
    }

    public String getStatusString() {
        return status != null ? status.toString() : "UNKNOWN";
    }
}
