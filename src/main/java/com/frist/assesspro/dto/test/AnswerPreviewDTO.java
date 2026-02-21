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
}