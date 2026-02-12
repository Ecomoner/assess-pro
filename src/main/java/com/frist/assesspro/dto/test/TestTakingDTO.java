package com.frist.assesspro.dto.test;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestTakingDTO {
    private Long attemptId;
    private Long testId;
    private String testTitle;
    private Integer timeLimitMinutes;
    private List<QuestionForTakingDTO> questions = new ArrayList<>();
    private Integer currentQuestionIndex = 0;
    private Integer totalQuestions;

    public boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }
}