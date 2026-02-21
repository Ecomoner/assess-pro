package com.frist.assesspro.dto.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestPreviewDTO {
    private Long id;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Long creatorId;
    private String creatorUsername;
    private List<QuestionPreviewDTO> questions = new ArrayList<>();

    // Конструктор для Hibernate (без коллекции)
    public TestPreviewDTO(Long id, String title, String description,
                          Integer timeLimitMinutes, Long creatorId, String creatorUsername) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.timeLimitMinutes = timeLimitMinutes;
        this.creatorId = creatorId;
        this.creatorUsername = creatorUsername;
        this.questions = new ArrayList<>();
    }

    public int getTotalQuestions() {
        return questions != null ? questions.size() : 0;
    }
}