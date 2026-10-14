package com.frist.assesspro.service;


import com.frist.assesspro.dto.notification.NotificationCreatedEvent;
import com.frist.assesspro.dto.notification.NotificationDTO;
import com.frist.assesspro.entity.Notification;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.mapper.NotificationMapper;
import com.frist.assesspro.repository.NotificationRepository;
import com.frist.assesspro.repository.TestAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final TestAttemptRepository  testAttemptRepository;
    private final ApplicationEventPublisher eventPublisher;


    public NotificationDTO createNotification(User recipient, String message,
                                              Notification.NotificationType type,
                                              Long relatedEntityId) {

        boolean alreadyExists = notificationRepository.existsByUserIdAndTypeAndRelatedEntityId(
                recipient.getId(), type, relatedEntityId);
        if (alreadyExists) {
            log.warn("Дублирующее уведомление для {} (тип {}, relatedEntityId {})",
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

        // Публикуем событие — обработчик сработает ПОСЛЕ коммита
        eventPublisher.publishEvent(new NotificationCreatedEvent(recipient.getId(), dto));

        return dto;
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        // Собираем ID попыток, для которых нужен URL
        Set<Long> attemptIds = unread.stream()
                .filter(n -> n.getType() == Notification.NotificationType.ATTEMPT_COMPLETED)
                .map(Notification::getRelatedEntityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Long> attemptToTestId = attemptIds.isEmpty()
                ? Map.of()
                : testAttemptRepository.findTestIdsByAttemptIds(attemptIds);

        return unread.stream()
                .map(n -> toDtoWithUrl(n, attemptToTestId))
                .toList();
    }

    private NotificationDTO toDtoWithUrl(Notification n, Map<Long, Long> attemptToTestId) {
        NotificationDTO dto = notificationMapper.toDto(n);
        dto.setTargetUrl(buildTargetUrl(n.getType(), n.getRelatedEntityId(), attemptToTestId));
        return dto;
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    public void deleteReadNotifications(Long userId) {
        notificationRepository.deleteByUserIdAndIsReadTrue(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
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

    private String buildTargetUrl(Notification.NotificationType type,
                                 Long relatedEntityId,
                                 Map<Long, Long> attemptToTestId) {
        if (relatedEntityId == null) return "#";
        switch (type) {
            case TEST_PUBLISHED:
                return "/tester/attempt/" + relatedEntityId;
            case MATERIAL_UPDATED:
                return "/materials";
            case ATTEMPT_COMPLETED:
                Long testId = attemptToTestId.get(relatedEntityId);
                return testId != null
                        ? "/manager/tests/" + testId + "/statistics/tester/" + relatedEntityId
                        : "#";
            case ASSIGNED_TO_PROJECT:
                return "/tester/dashboard";
            case TEST_STATUS_CHANGED:
                return "/creator/tests";
            default:
                return "#";
        }
    }

    @Transactional
    public List<NotificationDTO> createNotificationsBatch(
            List<User> recipients,
            String message,
            Notification.NotificationType type,
            Long relatedEntityId) {

        if (recipients.isEmpty()) return List.of();

        Set<Long> alreadyNotified = notificationRepository
                .findUserIdsByTypeAndRelatedEntityId(type, relatedEntityId);

        List<Notification> toSave = recipients.stream()
                .filter(u -> !alreadyNotified.contains(u.getId()))
                .map(u -> Notification.builder()
                        .user(u)
                        .message(message)
                        .type(type)
                        .relatedEntityId(relatedEntityId)
                        .build())
                .toList();

        if (toSave.isEmpty()) return List.of();

        List<Notification> saved = notificationRepository.saveAll(toSave);

        List<NotificationDTO> dtos = saved.stream()
                .map(n -> {
                    NotificationDTO dto = notificationMapper.toDto(n);
                    dto.setTargetUrl(buildTargetUrl(type, relatedEntityId));
                    return dto;
                })
                .toList();

        // Публикуем события после сохранения
        for (int i = 0; i < saved.size(); i++) {
            eventPublisher.publishEvent(new NotificationCreatedEvent(
                    saved.get(i).getUser().getId(), dtos.get(i)));
        }

        return dtos;
    }
}
