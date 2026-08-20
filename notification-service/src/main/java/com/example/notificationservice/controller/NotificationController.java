package com.example.notificationservice.controller;

import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(
        name = "Уведомления",
        description = "Просмотр и управление уведомлениями о доставках"
)
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "Получить список уведомлений",
            description = "Возвращает уведомления с возможностью фильтрации по доставке и статусу прочтения"
    )
    @GetMapping
    public List<NotificationResponse> getAllNotifications(
            @Parameter(description = "ID доставки")
            @RequestParam(required = false) Long deliveryId,

            @Parameter(description = "Статус прочтения уведомления")
            @RequestParam(required = false) Boolean read
    ) {
        return notificationService.getAllNotifications(
                deliveryId,
                read
        );
    }

    @Operation(
            summary = "Получить уведомление по ID"
    )
    @GetMapping("/{id}")
    public NotificationResponse getNotificationById(
            @Parameter(description = "ID уведомления", required = true)
            @PathVariable Long id
    ) {
        return notificationService.getNotificationById(id);
    }

    @Operation(
            summary = "Отметить уведомление как прочитанное",
            description = "Изменяет статус уведомления на read = true"
    )
    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @Parameter(description = "ID уведомления", required = true)
            @PathVariable Long id
    ) {
        return notificationService.markAsRead(id);
    }
}