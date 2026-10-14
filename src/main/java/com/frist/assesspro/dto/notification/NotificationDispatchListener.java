package com.frist.assesspro.dto.notification;

import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import com.frist.assesspro.service.EmailService;
import com.frist.assesspro.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchListener {

    private final SseService sseService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        NotificationDTO dto = event.dto();

        // SSE
        try {
            sseService.sendToUser(event.recipientId(), dto);
        } catch (Exception e) {
            log.error("SSE failed for user {}: {}", event.recipientId(), e.getMessage());
        }

        // Email
        try {
            User user = userRepository.findById(event.recipientId()).orElse(null);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                Map<String, Object> model = new HashMap<>();
                model.put("message", dto.getMessage());
                model.put("targetUrl", dto.getTargetUrl());
                emailService.sendEmail(user.getEmail(),
                        "Новое уведомление в AssessPro",
                        "email/notification", model);
            }
        } catch (Exception e) {
            log.error("Email failed for user {}: {}", event.recipientId(), e.getMessage());
        }
    }
}
