package com.example.notificationservice.dto;

import com.example.notificationservice.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(

        Long id,
        UUID eventId,
        Long deliveryId,
        Long customerId,
        NotificationType type,
        String message,
        boolean read,
        LocalDateTime createdAt

) {
}