package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import com.example.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long jwtExpirationSeconds;

    public AuthService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${jwt.expiration-seconds}") long jwtExpirationSeconds
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtExpirationSeconds = jwtExpirationSeconds;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return createUser(request, Role.CUSTOMER);
    }

    @Transactional
    public AuthResponse createCourier(RegisterRequest request) {
        return createUser(request, Role.COURIER);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                jwtExpirationSeconds
        );
    }

    private AuthResponse createUser(
            RegisterRequest request,
            Role role
    ) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "User with this email already exists"
            );
        }

        User user = userMapper.toEntity(
                request,
                role
        );

        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }
}