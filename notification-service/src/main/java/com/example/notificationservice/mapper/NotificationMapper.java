package com.example.notificationservice.mapper;

import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getDeliveryId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
