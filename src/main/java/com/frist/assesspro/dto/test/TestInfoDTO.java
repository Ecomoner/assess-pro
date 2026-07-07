package com.frist.assesspro.dto.test;

import com.frist.assesspro.validation.ValidDateRange;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidDateRange
public class TestInfoDTO {
    private Long id;
    private String title;
    private String description;
    private Integer questionCount;
    private Integer timeLimitMinutes;
    private LocalDateTime createdAt;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;

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
