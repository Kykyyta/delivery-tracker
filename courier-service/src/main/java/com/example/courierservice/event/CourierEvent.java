package com.example.courierservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourierEvent(

        UUID eventId,
        CourierEventType eventType,
        Long deliveryId,
        Long courierId,
        LocalDateTime occurredAt

) {
}