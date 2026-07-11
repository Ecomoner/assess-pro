package com.frist.assesspro.dto;


import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {

    private Long id;
    private String title;       // для FullCalendar
    private String description;
    private LocalDate start;    // для FullCalendar (дата)
    private String url;         // ссылка на детали
    private String color;       // цвет (можно задавать)
    private String createdByName;

}
