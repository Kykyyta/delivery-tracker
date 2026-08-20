package com.example.deliveryservice.dto;

import com.example.deliveryservice.model.DeliveryStatus;

import java.time.LocalDateTime;

public record DeliveryResponse(

        Long id,
        String customerName,
        String customerPhone,
        String pickupAddress,
        String deliveryAddress,
        DeliveryStatus status,
        Long courierId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}
