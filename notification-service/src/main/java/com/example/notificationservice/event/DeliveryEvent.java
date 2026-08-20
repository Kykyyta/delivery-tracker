package com.example.notificationservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryEvent(

        UUID eventId,
        DeliveryEventType eventType,
        Long deliveryId,
        LocalDateTime occurredAt

) {
}