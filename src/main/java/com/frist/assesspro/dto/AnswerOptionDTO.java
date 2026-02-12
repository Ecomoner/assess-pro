package com.frist.assesspro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnswerOptionDTO {
    private Long id;

    @NotBlank(message = "Текст варианта ответа обязателен")
    @Size(min = 1, max = 500, message = "Текст варианта ответа должен быть от 1 до 500 символов")
    private String text;

    @NotNull(message = "Поле isCorrect обязательно")
    private Boolean isCorrect = false;
}