package com.frist.assesspro.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_answers")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Попытка обязательна")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @NotNull(message = "Вопрос обязателен")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_answer_option_id")
    private AnswerOption chosenAnswerOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Min(value = 0, message = "Количество баллов не может быть отрицательным")
    @Column(name = "points_earned")
    private Integer pointsEarned = 0;
}
