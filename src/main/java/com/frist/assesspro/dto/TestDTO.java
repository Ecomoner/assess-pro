package com.frist.assesspro.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestDTO {

    private Long id;

    @NotBlank(message = "Название теста обязательно")
    @Size(min = 3, max = 200, message = "Название теста должно быть от 3 до 200 символов")
    private String title;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    private boolean published;
    private Long questionCount;
    private LocalDateTime createdAt;

    @Min(value = 0, message = "Лимит времени не может быть отрицательным")
    @Max(value = 300, message = "Максимальный лимит времени - 300 минут")
    private Integer timeLimitMinutes;


    @Min(value = 0, message = "Количество часов не может быть отрицательным")
    @Max(value = 336, message = "Максимальное ограничение - 14 дней (336 часов)")
    private Integer retryCooldownHours = 0;

    @Min(value = 0, message = "Количество дней не может быть отрицательным")
    @Max(value = 14, message = "Максимальное ограничение - 14 дней")
    private Integer retryCooldownDays = 0;

    private Long categoryId;
    private String categoryName;
    private Long creatorId;
    private String creatorUsername;

    public TestDTO(Long id, String title, String description, Boolean published,
                   Long questionCount, LocalDateTime createdAt, Integer timeLimitMinutes,
                   Long categoryId, String categoryName,
                   Long creatorId, String creatorUsername) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.published = published != null ? published : false;
        this.questionCount = questionCount != null ? questionCount : 0L;
        this.createdAt = createdAt;
        this.timeLimitMinutes = timeLimitMinutes != null ? timeLimitMinutes : 0;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.creatorId = creatorId;
        this.creatorUsername = creatorUsername;
    }

    // Перегруженный конструктор для случаев, когда нужны cooldown поля
    public TestDTO(Long id, String title, String description, Boolean published,
                   Long questionCount, LocalDateTime createdAt, Integer timeLimitMinutes,
                   Integer retryCooldownHours, Integer retryCooldownDays,
                   Long categoryId, String categoryName,
                   Long creatorId, String creatorUsername) {
        this(id, title, description, published, questionCount, createdAt, timeLimitMinutes,
                categoryId, categoryName, creatorId, creatorUsername);
        this.retryCooldownHours = retryCooldownHours != null ? retryCooldownHours : 0;
        this.retryCooldownDays = retryCooldownDays != null ? retryCooldownDays : 0;
    }


}
