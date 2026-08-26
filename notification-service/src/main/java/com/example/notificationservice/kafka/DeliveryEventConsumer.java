package com.example.notificationservice.kafka;

import com.example.notificationservice.event.DeliveryEvent;
import com.example.notificationservice.model.NotificationType;
import com.example.notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventConsumer {

    private final NotificationService notificationService;

    public DeliveryEventConsumer(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "delivery.events",
            containerFactory = "deliveryEventKafkaListenerContainerFactory"
    )
    public void handleDeliveryEvent(DeliveryEvent event) {

        switch (event.eventType()) {

            case DELIVERY_CREATED ->
                    notificationService.createNotification(
                            event.eventId(),
                            event.deliveryId(),
                            event.customerId(),
                            NotificationType.DELIVERY_CREATED,
                            "Доставка #" + event.deliveryId() + " создана"
                    );

            case DELIVERY_PICKED_UP ->
                    notificationService.createNotification(
                            event.eventId(),
                            event.deliveryId(),
                            event.customerId(),
                            NotificationType.DELIVERY_PICKED_UP,
                            "Курьер забрал доставку #" + event.deliveryId()
                    );

            case DELIVERY_COMPLETED ->
                    notificationService.createNotification(
                            event.eventId(),
                            event.deliveryId(),
                            event.customerId(),
                            NotificationType.DELIVERY_COMPLETED,
                            "Доставка #" + event.deliveryId()
                                    + " успешно завершена"
                    );

            case DELIVERY_CANCELLED ->
                    notificationService.createNotification(
                            event.eventId(),
                            event.deliveryId(),
                            event.customerId(),
                            NotificationType.DELIVERY_CANCELLED,
                            "Доставка #" + event.deliveryId()
                                    + " отменена"
                    );
        }
    }
}