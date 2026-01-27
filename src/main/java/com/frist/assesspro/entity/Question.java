package com.frist.assesspro.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "questions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Тест обязателен")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @NotBlank(message = "Текст вопроса обязателен")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Min(value = 0, message = "Порядковый номер не может быть отрицательным")
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerOption> answerOptions;

    @OneToMany(mappedBy = "question")
    private List<UserAnswer> userAnswers;
}
