package com.example.courierservice.controller;

import com.example.courierservice.dto.CourierRequest;
import com.example.courierservice.dto.CourierResponse;
import com.example.courierservice.model.CourierStatus;
import com.example.courierservice.service.CourierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
            @Valid @RequestBody CourierRequest request
    ) {
        return courierService.createCourier(request);
    }

    @Operation(
            summary = "Получить список курьеров",
            description = "Возвращает всех курьеров с возможностью фильтрации по статусу"
    )
    @GetMapping
    public List<CourierResponse> getAllCouriers(
            @Parameter(description = "Статус курьера")
            @RequestParam(required = false) CourierStatus status
    ) {
        return courierService.getAllCouriers(status);
    }

    @Operation(
            summary = "Получить курьера по ID",
            description = "Возвращает информацию о конкретном курьере"
    )
    @GetMapping("/{id}")
    public CourierResponse getCourierById(
            @Parameter(description = "ID курьера", required = true)
            @PathVariable Long id
    ) {
        return courierService.getCourierById(id);
    }

    @Operation(
            summary = "Изменить данные курьера",
            description = "Изменяет имя и телефон курьера"
    )
    @PutMapping("/{id}")
    public CourierResponse updateCourier(
            @Parameter(description = "ID курьера", required = true)
            @PathVariable Long id,

            @Valid @RequestBody CourierRequest request
    ) {
        return courierService.updateCourier(id, request);
    }

    @Operation(
            summary = "Перевести курьера в OFFLINE",
            description = "Переводит свободного курьера из AVAILABLE в OFFLINE"
    )
    @PatchMapping("/{id}/offline")
    public CourierResponse goOffline(
            @Parameter(description = "ID курьера", required = true)
            @PathVariable Long id
    ) {
        return courierService.goOffline(id);
    }

    @Operation(
            summary = "Вернуть курьера в работу",
            description = "Переводит курьера из OFFLINE в AVAILABLE"
    )
    @PatchMapping("/{id}/online")
    public CourierResponse goOnline(
            @Parameter(description = "ID курьера", required = true)
            @PathVariable Long id
    ) {
        return courierService.goOnline(id);
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
}