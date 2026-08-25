package com.example.deliveryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return authenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/actuator/health")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/deliveries",
                                "/api/deliveries/"
                        )
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/deliveries/*/pickup",
                                "/api/deliveries/*/complete"
                        )
                        .hasAnyRole("COURIER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/deliveries/*/cancel"
                        )
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/deliveries/**"
                        )
                        .hasAnyRole("CUSTOMER", "COURIER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/deliveries/**"
                        )
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/deliveries/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers("/api/deliveries/**")
                        .denyAll()

                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }
}