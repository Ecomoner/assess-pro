package com.frist.assesspro.repository;

import com.frist.assesspro.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface NotificationRepository extends JpaRepository<Notification,Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(Long id);

    void deleteByUserIdAndIsReadTrue(Long userId);

    boolean existsByUserIdAndTypeAndRelatedEntityId(Long id, Notification.NotificationType type, Long relatedEntityId);

    @Query("SELECT n.user.id FROM Notification n " +
            "WHERE n.type = :type AND n.relatedEntityId = :relatedEntityId")
    Set<Long> findUserIdsByTypeAndRelatedEntityId(
            @Param("type") Notification.NotificationType type,
            @Param("relatedEntityId") Long relatedEntityId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}
