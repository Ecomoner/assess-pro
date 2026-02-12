package com.frist.assesspro.dto.test;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestRetryCooldownDTO {

    @Min(value = 0, message = "Количество часов не может быть отрицательным")
    @Max(value = 336, message = "Максимальное ограничение - 14 дней (336 часов)")
    private Integer cooldownHours = 0;

    @Min(value = 0, message = "Количество дней не может быть отрицательным")
    @Max(value = 14, message = "Максимальное ограничение - 14 дней")
    private Integer cooldownDays = 0;

    /**
     * Получение эффективного значения в часах
     */
    public int getEffectiveHours() {
        if (cooldownDays != null && cooldownDays > 0) {
            return cooldownDays * 24;
        }
        return cooldownHours != null ? cooldownHours : 0;
    }
}
