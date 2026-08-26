package com.example.deliveryservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourierEvent(

        UUID eventId,
        CourierEventType eventType,
        Long deliveryId,
        Long courierId,
        Long courierUserId,
        Long customerId,
        LocalDateTime occurredAt

) {
}