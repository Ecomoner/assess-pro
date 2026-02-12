package com.frist.assesspro.dto.test;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TestUpdateDTO {

    private Long id;

    @NotBlank(message = "Название теста обязательно")
    @Size(min = 3, max = 200, message = "Название теста должно быть от 3 до 200 символов")
    private String title;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    private Integer timeLimitMinutes;

    private Long categoryId;

    @Min(0)
    @Max(336)
    private Integer retryCooldownHours = 0;

    @Min(0)
    @Max(14)
    private Integer retryCooldownDays = 0;
}