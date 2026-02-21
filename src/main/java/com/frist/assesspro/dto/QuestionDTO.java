package com.frist.assesspro.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionDTO {
    private Long id;

    @NotBlank(message = "Текст вопроса обязателен")
    private String text;

    @NotNull(message = "Порядковый номер обязателен")
    @Min(value = 0, message = "Порядковый номер не может быть отрицательным")
    private Integer orderIndex = 0;

    @NotNull(message = "Список вариантов ответа обязателен")
    private List<AnswerOptionDTO> answerOptions = new ArrayList<>();

    public QuestionDTO() {

    }

    public QuestionDTO(Long id, String text, Integer orderIndex,
                       List<AnswerOptionDTO> answerOptions) {
        this.id = id;
        this.text = text;
        this.orderIndex = orderIndex;
        this.answerOptions = answerOptions != null ? answerOptions : new ArrayList<>();
    }
}