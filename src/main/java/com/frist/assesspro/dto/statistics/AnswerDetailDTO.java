package com.frist.assesspro.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerDetailDTO {
    private Long answerId;
    private String answerText;
    private Boolean isCorrect;
}
