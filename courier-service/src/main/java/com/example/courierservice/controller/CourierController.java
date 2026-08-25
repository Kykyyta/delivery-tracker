package com.example.courierservice.controller;

import com.example.courierservice.dto.CourierCreateRequest;
import com.example.courierservice.dto.CourierRequest;
import com.example.courierservice.dto.CourierResponse;
import com.example.courierservice.model.CourierStatus;
import com.example.courierservice.service.CourierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/couriers")
@Tag(
        name = "Курьеры",
        description = "Управление курьерами и их рабочими статусами"
)
public class CourierController {

    private final CourierService courierService;

    public CourierController(CourierService courierService) {
        this.courierService = courierService;
    }

    @Operation(
            summary = "Создать курьера",
            description = "Создаёт нового курьера со статусом AVAILABLE"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourierResponse createCourier(
            @Valid @RequestBody CourierCreateRequest request
    ) {
        return courierService.createCourier(request);
    }

    @Operation(
            summary = "Получить список курьеров",
            description = "Возвращает курьеров с возможностью фильтрации по статусу"
    )
    @GetMapping
    public List<CourierResponse> getAllCouriers(
            @Parameter(description = "Статус курьера")
            @RequestParam(required = false) CourierStatus status,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return courierService.getAllCouriers(
                status,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Получить курьера по ID",
            description = "Возвращает информацию о конкретном курьере"
    )
    @GetMapping("/{id}")
    public CourierResponse getCourierById(
            @Parameter(description = "ID курьера", required = true)
            @PathVariable Long id,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return courierService.getCourierById(
                id,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Изменить данные курьера",
            description = "Изменяет имя и телефон курьера"
    )
    @PutMapping("/{id}")
    public CourierResponse updateCourier(
            @Parameter(description = "ID курьера", required = true)
            @PathVariable Long id,

            @Valid @RequestBody CourierRequest request,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return courierService.updateCourier(
                id,
                request,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Перевести курьера в OFFLINE",
            description = "Переводит свободного курьера из AVAILABLE в OFFLINE"
    )
    @PatchMapping("/{id}/offline")
    public CourierResponse goOffline(
            @Parameter(description = "ID курьера", required = true)
            @PathVariable Long id,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return courierService.goOffline(
                id,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Вернуть курьера в работу",
            description = "Переводит курьера из OFFLINE в AVAILABLE"
    )
    @PatchMapping("/{id}/online")
    public CourierResponse goOnline(
            @Parameter(description = "ID курьера", required = true)
            @PathVariable Long id,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return courierService.goOnline(
                id,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Удалить курьера",
            description = "Удаляет курьера, если он не выполняет доставку"
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourier(
            @Parameter(description = "ID курьера", required = true)
            @PathVariable Long id
    ) {
        courierService.deleteCourier(id);
    }

    private Long getUserId(Jwt jwt) {
        Number userId = jwt.getClaim("userId");

        return userId.longValue();
    }

    private String getRole(Jwt jwt) {
        return jwt.getClaimAsString("role");
    }
}