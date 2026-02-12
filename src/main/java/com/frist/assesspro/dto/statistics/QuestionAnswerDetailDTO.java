package com.frist.assesspro.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionAnswerDetailDTO {
    private Long questionId;
    private String questionText;
    private Integer questionOrder;
    private AnswerDetailDTO chosenAnswer;
    private AnswerDetailDTO correctAnswer;
    private Boolean isCorrect;
    private Integer pointsEarned;

    /**
     * Получает CSS класс для ответа
     */
    public String getAnswerClass() {
        if (isCorrect == null) {
            return "border-secondary";
        }
        return isCorrect ? "border-success" : "border-danger";
    }

    /**
     * Получает иконку для ответа
     */
    public String getAnswerIcon() {
        if (isCorrect == null) {
            return "bi-question-circle";
        }
        return isCorrect ? "bi-check-circle" : "bi-x-circle";
    }

    /**
     * Получает текст статуса
     */
    public String getAnswerStatus() {
        if (chosenAnswer == null) {
            return "Не отвечено";
        }
        return isCorrect ? "Правильно" : "Неправильно";
    }

    /**
     * Проверяет, был ли ответ выбран
     */
    public boolean isAnswered() {
        return chosenAnswer != null;
    }


}