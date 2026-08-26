package com.example.courierservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryEvent(

        UUID eventId,
        DeliveryEventType eventType,
        Long deliveryId,
        Long customerId,
        LocalDateTime occurredAt

) {
}