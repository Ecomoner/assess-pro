package com.frist.assesspro.dto.statistics;

import java.time.LocalDateTime;

public interface TesterProjectStatsProjection {
    Long getUserId();
    String getUsername();
    String getFirstName();
    String getLastName();
    Long getAttemptCount();
    Double getAverageScore();
    LocalDateTime getLastAttemptDate();

    default String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (getLastName() != null) sb.append(getLastName());
        if (getFirstName() != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(getFirstName());
        }
        return sb.isEmpty() ? getUsername() : sb.toString();
    }
}
