package com.frist.assesspro.dto.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerPreviewDTO {
    private Long id;
    private String text;
    private Boolean isCorrect;
    private Long questionId;

    public AnswerPreviewDTO(Long id, String text, Boolean isCorrect) {
        this.id = id;
        this.text = text;
        this.isCorrect = isCorrect;
    }
}