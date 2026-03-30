package com.frist.assesspro.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "events")
@AllArgsConstructor
@NoArgsConstructor
@Cacheable
public class Event {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название события обязательно")
    @Size(min = 2, max = 100, message = "Название события должно быть от 2 до 100 символов")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Описание события обязательно")
    @Size(max = 1000, message = "Описание события должно быть до 1000 знаков")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Создатель обязателен")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_event", nullable = false)
    private User createdByEvent;
}
