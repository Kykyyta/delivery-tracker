package com.example.notificationservice.kafka;

import com.example.notificationservice.event.CourierEvent;
import com.example.notificationservice.event.CourierEventType;
import com.example.notificationservice.model.NotificationType;
import com.example.notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CourierEventConsumer {

    private final NotificationService notificationService;

    public CourierEventConsumer(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "courier.events",
            containerFactory = "courierEventKafkaListenerContainerFactory"
    )
    public void handleCourierEvent(CourierEvent event) {

        if (event.eventType() == CourierEventType.COURIER_ASSIGNED) {

            notificationService.createNotification(
                    event.eventId(),
                    event.deliveryId(),
                    NotificationType.COURIER_ASSIGNED,
                    "К доставке #" + event.deliveryId()
                            + " назначен курьер #" + event.courierId()
            );
        }
    }
}