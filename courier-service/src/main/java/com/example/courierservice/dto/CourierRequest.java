package com.example.courierservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourierRequest(

        @NotBlank(message = "Имя курьера не должно быть пустым")
        @Size(max = 100, message = "Имя курьера не должно превышать 100 символов")
        String name,

        @NotBlank(message = "Телефон курьера не должен быть пустым")
        @Size(max = 30, message = "Телефон курьера не должен превышать 30 символов")
        String phone

) {
}
