package com.frist.assesspro.dto.test;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionForTakingDTO {
    private Long id;
    private String text;
    private Integer orderIndex;
    private List<AnswerOptionForTakingDTO> answerOptions = new ArrayList<>();

    @Data
    public static class AnswerOptionForTakingDTO {
        private Long id;
        private String text;
    }
}
