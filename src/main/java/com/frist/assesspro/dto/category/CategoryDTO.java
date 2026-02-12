package com.frist.assesspro.dto.category;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Long testsCount;
    private Long publishedTestsCount;

    public CategoryDTO(Long id, String name, String description,
                       LocalDateTime createdAt, Long testsCount, Long publishedTestsCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.testsCount = testsCount != null ? testsCount : 0L;
        this.publishedTestsCount = publishedTestsCount != null ? publishedTestsCount : 0L;
    }
}