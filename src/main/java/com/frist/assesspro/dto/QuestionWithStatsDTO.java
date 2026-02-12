package com.frist.assesspro.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class QuestionWithStatsDTO {
    private Long id;
    private String text;
    private Integer orderIndex;
    private List<AnswerOptionDTO> answerOptions;

    // Статистические поля - вычисляются в сервисе
    private Integer totalAnswerOptions;
    private Integer correctAnswerOptions;
    private Integer incorrectAnswerOptions;
    private List<String> correctAnswerTexts;
    private List<String> incorrectAnswerTexts;

    /**
     * Конструктор с автоматическим вычислением статистики
     * Теперь статистика вычисляется явно через вызов метода
     */
    public QuestionWithStatsDTO(Long id, String text, Integer orderIndex,
                                List<AnswerOptionDTO> answerOptions) {
        this.id = id;
        this.text = text;
        this.orderIndex = orderIndex;
        this.answerOptions = answerOptions != null ? answerOptions : new ArrayList<>();
        // Убрали автоматический вызов calculateStats()
    }

    /**
     * Вычисление статистики - вызывается явно в сервисе
     */
    public void calculateStats() {
        // Фильтруем пустые варианты ответов
        List<AnswerOptionDTO> validAnswers = answerOptions.stream()
                .filter(answer -> answer.getText() != null && !answer.getText().trim().isEmpty())
                .collect(Collectors.toList());

        if (validAnswers.isEmpty()) {
            this.totalAnswerOptions = 0;
            this.correctAnswerOptions = 0;
            this.incorrectAnswerOptions = 0;
            this.correctAnswerTexts = List.of();
            this.incorrectAnswerTexts = List.of();
            return;
        }

        this.totalAnswerOptions = validAnswers.size();
        this.correctAnswerOptions = (int) validAnswers.stream()
                .filter(AnswerOptionDTO::getIsCorrect)
                .count();
        this.incorrectAnswerOptions = totalAnswerOptions - correctAnswerOptions;

        this.correctAnswerTexts = validAnswers.stream()
                .filter(AnswerOptionDTO::getIsCorrect)
                .map(AnswerOptionDTO::getText)
                .collect(Collectors.toList());

        this.incorrectAnswerTexts = validAnswers.stream()
                .filter(answer -> !answer.getIsCorrect())
                .map(AnswerOptionDTO::getText)
                .collect(Collectors.toList());
    }

    /**
     * Статический метод для создания DTO со статистикой
     */
    public static QuestionWithStatsDTO createWithStats(Long id, String text, Integer orderIndex,
                                                       List<AnswerOptionDTO> answerOptions) {
        QuestionWithStatsDTO dto = new QuestionWithStatsDTO(id, text, orderIndex, answerOptions);
        dto.calculateStats();
        return dto;
    }
}