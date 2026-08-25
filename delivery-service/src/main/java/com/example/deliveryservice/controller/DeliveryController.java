package com.example.deliveryservice.controller;

import com.example.deliveryservice.dto.DeliveryRequest;
import com.example.deliveryservice.dto.DeliveryResponse;
import com.example.deliveryservice.model.DeliveryStatus;
import com.example.deliveryservice.service.DeliveryService;
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
@RequestMapping("/api/deliveries")
@Tag(
        name = "Доставки",
        description = "Управление доставками и их жизненным циклом"
)
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Operation(
            summary = "Создать доставку",
            description = "Создаёт новую доставку со статусом CREATED"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryResponse createDelivery(
            @Valid @RequestBody DeliveryRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return deliveryService.createDelivery(
                request,
                getUserId(jwt)
        );
    }

    @Operation(
            summary = "Получить список доставок",
            description = "Возвращает доставки с возможностью фильтрации"
    )
    @GetMapping
    public List<DeliveryResponse> getAllDeliveries(
            @Parameter(description = "Статус доставки")
            @RequestParam(required = false) DeliveryStatus status,

            @Parameter(description = "ID назначенного курьера")
            @RequestParam(required = false) Long courierId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return deliveryService.getAllDeliveries(
                status,
                courierId,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Получить доставку по ID",
            description = "Возвращает информацию о конкретной доставке"
    )
    @GetMapping("/{id}")
    public DeliveryResponse getDeliveryById(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return deliveryService.getDeliveryById(
                id,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Изменить данные доставки",
            description = "Изменяет данные клиента и адреса доставки"
    )
    @PutMapping("/{id}")
    public DeliveryResponse updateDelivery(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id,

            @Valid @RequestBody DeliveryRequest request,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return deliveryService.updateDelivery(
                id,
                request,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Отметить доставку как забранную",
            description = "Переводит доставку из статуса COURIER_ASSIGNED в PICKED_UP"
    )
    @PatchMapping("/{id}/pickup")
    public DeliveryResponse pickupDelivery(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return deliveryService.pickupDelivery(
                id,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Завершить доставку",
            description = "Переводит доставку из статуса PICKED_UP в COMPLETED"
    )
    @PatchMapping("/{id}/complete")
    public DeliveryResponse completeDelivery(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return deliveryService.completeDelivery(
                id,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Отменить доставку",
            description = "Отменяет доставку, если она находится в статусе CREATED или COURIER_ASSIGNED"
    )
    @PatchMapping("/{id}/cancel")
    public DeliveryResponse cancelDelivery(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return deliveryService.cancelDelivery(
                id,
                getUserId(jwt),
                getRole(jwt)
        );
    }

    @Operation(
            summary = "Удалить доставку",
            description = "Полностью удаляет доставку из системы"
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDelivery(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id
    ) {
        deliveryService.deleteDelivery(id);
    }

    private Long getUserId(Jwt jwt) {
        Number userId = jwt.getClaim("userId");

        return userId.longValue();
    }

    private String getRole(Jwt jwt) {
        return jwt.getClaimAsString("role");
    }
}