package com.frist.assesspro.service;


import com.frist.assesspro.dto.notification.NotificationDTO;
import com.frist.assesspro.entity.Notification;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.mapper.NotificationMapper;
import com.frist.assesspro.repository.NotificationRepository;
import com.frist.assesspro.repository.TestAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SseService sseService;
    private final TestAttemptRepository  testAttemptRepository;
    private final EmailService emailService;

    @Transactional
    public NotificationDTO createNotification(User recipient, String message,
                                              Notification.NotificationType type,
                                              Long relatedEntityId) {

        boolean alreadyExists = notificationRepository.existsByUserIdAndTypeAndRelatedEntityId(
                recipient.getId(), type, relatedEntityId);
        if (alreadyExists) {
            log.warn("Попытка создать дублирующее уведомление для пользователя {} (тип {}, relatedEntityId {})",
                    recipient.getUsername(), type, relatedEntityId);
            return null;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .message(message)
                .type(type)
                .relatedEntityId(relatedEntityId)
                .build();
        notification = notificationRepository.save(notification);
        NotificationDTO dto = notificationMapper.toDto(notification);
        dto.setTargetUrl(buildTargetUrl(type, relatedEntityId));

        try {
            sseService.sendToUser(recipient.getId(), dto);

            if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
                try {
                    Map<String, Object> model = new HashMap<>();
                    model.put("message", message);
                    model.put("targetUrl", dto.getTargetUrl());
                    emailService.sendEmail(recipient.getEmail(), "Новое уведомление в AssessPro",
                            "email/notification", model);
                } catch (Exception e) {
                    log.error("Failed to queue email for user {}: {}", recipient.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Не удалось отправить SSE-уведомление пользователю {}: {}", recipient.getId(), e.getMessage());
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !n.isRead())
                .map(this::toDtoWithUrl)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    public void deleteReadNotifications(Long userId) {
        notificationRepository.deleteByUserIdAndIsReadTrue(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationDTO toDtoWithUrl(Notification n) {
        NotificationDTO dto = notificationMapper.toDto(n);
        dto.setTargetUrl(buildTargetUrl(n.getType(), n.getRelatedEntityId()));
        return dto;
    }

    private String buildTargetUrl(Notification.NotificationType type, Long relatedEntityId) {
        if (relatedEntityId == null) return "#";
        switch (type) {
            case TEST_PUBLISHED:
                return "/tester/attempt/" + relatedEntityId;
            case MATERIAL_UPDATED:
                return "/materials";
            case ATTEMPT_COMPLETED:
                // Ищем попытку, чтобы узнать testId
                return testAttemptRepository.findById(relatedEntityId)
                        .map(attempt -> "/manager/tests/" + attempt.getTest().getId() +
                                "/statistics/tester/" + relatedEntityId)
                        .orElse("#");
            case ASSIGNED_TO_PROJECT:
                return "/tester/dashboard";
            case TEST_STATUS_CHANGED:
                return "/creator/tests";
            default:
                return "#";
        }
    }
}
