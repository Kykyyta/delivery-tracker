package com.example.courierservice.dto;

import com.example.courierservice.model.CourierStatus;

import java.time.LocalDateTime;

public record CourierResponse(

        Long id,
        String name,
        String phone,
        CourierStatus status,
        Long currentDeliveryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}
