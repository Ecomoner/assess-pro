package com.frist.assesspro.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;                 // получатель

    @Column(nullable = false)
    private String message;            // текст уведомления

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;     // тип события

    @Column(name = "related_entity_id")
    private Long relatedEntityId;      // ID связанного объекта (тест, попытка, материал)

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        TEST_PUBLISHED,            // тестировщику: новый тест
        MATERIAL_UPDATED,          // тестировщику: обновлён материал
        ATTEMPT_COMPLETED,         // менеджеру/создателю: попытка завершена
        ASSIGNED_TO_PROJECT,       // тестировщику: назначен в проект
        TEST_STATUS_CHANGED,       // менеджеру: тест опубликован/снят
        GENERAL                    // универсальное
    }
}
