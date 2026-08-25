package com.example.authservice.dto;

import java.time.LocalDateTime;

public record AuthResponse(
        Long id,
        String name,
        String email,
        String role,
        LocalDateTime createdAt
) {
}