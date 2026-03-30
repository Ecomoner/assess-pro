package com.frist.assesspro.dto;


import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {

    private Long id;

    @NotBlank(message = "Название события обязательно")
    @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
    private String name;

    @NotBlank(message = "Описание события обязательно")
    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

}
