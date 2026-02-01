package com.frist.assesspro.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TestDTO {

    private Long id;
    private String title;
    private String description;
    private boolean published;
    private int questionCount;
    private LocalDateTime createdAt;
}
