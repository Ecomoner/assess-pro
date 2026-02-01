package com.frist.assesspro.dto.test;

import lombok.Data;

@Data
public class QuestionResultDTO {
    private Long questionId;
    private String questionText;
    private Long chosenAnswerId;
    private String chosenAnswerText;
    private Long correctAnswerId;
    private String correctAnswerText;
    private Boolean isCorrect;
    private Integer pointsEarned;
}
