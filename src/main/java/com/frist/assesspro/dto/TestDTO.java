package com.frist.assesspro.dto;


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
    private Integer timeLimitMinutes;
    private Long categoryId;
    private String categoryName;



}
