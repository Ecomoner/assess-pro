package com.frist.assesspro.dto.notification;

public record NotificationCreatedEvent(Long recipientId,NotificationDTO dto) {
}
