package com.frist.assesspro.dto.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPreviewDTO {
    private Long id;
    private String text;
    private Integer orderIndex;
    private List<AnswerPreviewDTO> answerOptions = new ArrayList<>();

    // Конструктор для Hibernate (без коллекции)
    public QuestionPreviewDTO(Long id, String text, Integer orderIndex) {
        this.id = id;
        this.text = text;
        this.orderIndex = orderIndex;
        this.answerOptions = new ArrayList<>();
    }
}