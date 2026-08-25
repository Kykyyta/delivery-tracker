package com.example.authservice.mapper;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(
            RegisterRequest request,
            Role role
    ) {
        User user = new User();

        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(role);

        return user;
    }

    public AuthResponse toResponse(User user) {
        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}