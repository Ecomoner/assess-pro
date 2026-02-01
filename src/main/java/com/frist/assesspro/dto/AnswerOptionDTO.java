package com.frist.assesspro.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnswerOptionDTO {
    private Long id;

    @NotBlank(message = "Текст варианта ответа обязателен")
    private String text;

    private Boolean isCorrect = false;
}