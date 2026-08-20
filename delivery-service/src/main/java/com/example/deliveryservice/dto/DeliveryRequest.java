package com.example.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryRequest(

        @NotBlank(message = "Имя клиента не должно быть пустым")
        @Size(max = 100, message = "Имя клиента не должно превышать 100 символов")
        String customerName,

        @NotBlank(message = "Телефон клиента не должен быть пустым")
        @Size(max = 30, message = "Телефон клиента не должен превышать 30 символов")
        String customerPhone,

        @NotBlank(message = "Адрес получения не должен быть пустым")
        @Size(max = 300, message = "Адрес получения не должен превышать 300 символов")
        String pickupAddress,

        @NotBlank(message = "Адрес доставки не должен быть пустым")
        @Size(max = 300, message = "Адрес доставки не должен превышать 300 символов")
        String deliveryAddress

) {
}
