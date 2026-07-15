package com.frist.assesspro.repository;

import com.frist.assesspro.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(Long id);

    void deleteByUserIdAndIsReadTrue(Long userId);

    boolean existsByUserIdAndTypeAndRelatedEntityId(Long id, Notification.NotificationType type, Long relatedEntityId);
}
