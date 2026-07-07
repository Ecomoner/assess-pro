package com.frist.assesspro.dto.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private String message;
    private String type;          // можно строкой или enum
    private Long relatedEntityId;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String targetUrl;     // ссылка для перехода
}