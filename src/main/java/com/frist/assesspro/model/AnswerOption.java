package com.frist.assesspro.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answer_options")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Вопрос обязателен")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @NotBlank(message = "Текст варианта ответа обязателен")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @NotNull(message = "Поле isCorrect обязательно")
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;
}
