package com.frist.assesspro.dto.test;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestInfoDTO {
    private Long id;
    private String title;
    private String description;
    private Integer questionCount;
    private Integer timeLimitMinutes;
    private LocalDateTime createdAt;

    public boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }
}
