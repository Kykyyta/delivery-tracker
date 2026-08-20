package com.example.deliveryservice.kafka;

import com.example.deliveryservice.event.CourierEvent;
import com.example.deliveryservice.event.CourierEventType;
import com.example.deliveryservice.service.DeliveryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CourierEventConsumer {

    private final DeliveryService deliveryService;

    public CourierEventConsumer(
            DeliveryService deliveryService
    ) {
        this.deliveryService = deliveryService;
    }

    @KafkaListener(topics = "courier.events")
    public void handleCourierEvent(CourierEvent event) {

        if (event.eventType() == CourierEventType.COURIER_ASSIGNED) {

            deliveryService.assignCourier(
                    event.deliveryId(),
                    event.courierId()
            );
        }
    }
}