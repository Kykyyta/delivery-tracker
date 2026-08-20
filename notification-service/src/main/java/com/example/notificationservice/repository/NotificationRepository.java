package com.example.notificationservice.repository;

import com.example.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventId(UUID eventId);

    List<Notification> findByDeliveryId(Long deliveryId);

    List<Notification> findByRead(boolean read);

    List<Notification> findByDeliveryIdAndRead(
            Long deliveryId,
            boolean read
    );
}
