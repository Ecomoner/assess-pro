package com.frist.assesspro.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "answer_options")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Cacheable

public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Вопрос обязателен")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @NotBlank(message = "Текст варианта ответа обязателен")
    @Size(min = 1, max = 500, message = "Текст варианта ответа должен быть от 1 до 500 символов")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @NotNull(message = "Поле isCorrect обязательно")
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;
}
