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
            @Valid @RequestBody DeliveryRequest request
    ) {
        return deliveryService.createDelivery(request);
    }

    @Operation(
            summary = "Получить список доставок",
            description = "Возвращает все доставки с возможностью фильтрации по статусу и ID курьера"
    )
    @GetMapping
    public List<DeliveryResponse> getAllDeliveries(
            @Parameter(description = "Статус доставки")
            @RequestParam(required = false) DeliveryStatus status,

            @Parameter(description = "ID назначенного курьера")
            @RequestParam(required = false) Long courierId
    ) {
        return deliveryService.getAllDeliveries(status, courierId);
    }

    @Operation(
            summary = "Получить доставку по ID",
            description = "Возвращает информацию о конкретной доставке"
    )
    @GetMapping("/{id}")
    public DeliveryResponse getDeliveryById(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id
    ) {
        return deliveryService.getDeliveryById(id);
    }

    @Operation(
            summary = "Изменить данные доставки",
            description = "Изменяет данные клиента и адреса доставки"
    )
    @PutMapping("/{id}")
    public DeliveryResponse updateDelivery(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id,

            @Valid @RequestBody DeliveryRequest request
    ) {
        return deliveryService.updateDelivery(id, request);
    }

    @Operation(
            summary = "Отметить доставку как забранную",
            description = "Переводит доставку из статуса COURIER_ASSIGNED в PICKED_UP"
    )
    @PatchMapping("/{id}/pickup")
    public DeliveryResponse pickupDelivery(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id
    ) {
        return deliveryService.pickupDelivery(id);
    }

    @Operation(
            summary = "Завершить доставку",
            description = "Переводит доставку из статуса PICKED_UP в COMPLETED"
    )
    @PatchMapping("/{id}/complete")
    public DeliveryResponse completeDelivery(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id
    ) {
        return deliveryService.completeDelivery(id);
    }

    @Operation(
            summary = "Отменить доставку",
            description = "Отменяет доставку, если она находится в статусе CREATED или COURIER_ASSIGNED"
    )
    @PatchMapping("/{id}/cancel")
    public DeliveryResponse cancelDelivery(
            @Parameter(description = "ID доставки", required = true)
            @PathVariable Long id
    ) {
        return deliveryService.cancelDelivery(id);
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
}
