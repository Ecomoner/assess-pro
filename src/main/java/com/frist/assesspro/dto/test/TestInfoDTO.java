package com.frist.assesspro.dto.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestInfoDTO {
    private Long id;
    private String title;
    private String description;
    private Long questionCount;
    private Integer timeLimitMinutes;
    private LocalDateTime createdAt;
    private Long categoryId;
    private String categoryName;

    public boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }

    public String getSafeCategoryName() {
        return categoryName != null ? categoryName : "Без категории";
    }

    /**
     *  Проверка наличия категории
     */
    public boolean hasCategory() {
        return categoryId != null && categoryName != null;
    }

    /**
     *  Форматированное отображение
     */
    public String getCategoryDisplay() {
        return hasCategory() ? categoryName : "Без категории";
    }
}
