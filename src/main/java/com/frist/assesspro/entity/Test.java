package com.frist.assesspro.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tests")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название теста обязательно")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Создатель обязателен")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Min(value = 0, message = "Лимит времени не может быть отрицательным")
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes = 0;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @ToString.Exclude
    private List<Question> questions;

    @OneToMany(mappedBy = "test")
    @ToString.Exclude
    private List<TestAttempt> attempts;
}