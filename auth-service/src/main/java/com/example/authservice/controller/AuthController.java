package com.example.authservice.controller;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/couriers")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse createCourier(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.createCourier(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public Map<String, Object> me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Map.of(
                "email", jwt.getSubject(),
                "userId", jwt.getClaim("userId"),
                "role", jwt.getClaim("role")
        );
    }

    @GetMapping("/customer-test")
    public String customerTest() {
        return "CUSTOMER access granted";
    }

    @GetMapping("/courier-test")
    public String courierTest() {
        return "COURIER access granted";
    }

    @GetMapping("/admin-test")
    public String adminTest() {
        return "ADMIN access granted";
    }
}