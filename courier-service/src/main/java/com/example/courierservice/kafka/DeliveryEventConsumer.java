package com.example.courierservice.kafka;

import com.example.courierservice.dto.CourierResponse;
import com.example.courierservice.event.DeliveryEvent;
import com.example.courierservice.event.DeliveryEventType;
import com.example.courierservice.service.CourierService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventConsumer {

    private final CourierService courierService;
    private final CourierEventProducer courierEventProducer;

    public DeliveryEventConsumer(
            CourierService courierService,
            CourierEventProducer courierEventProducer
    ) {
        this.courierService = courierService;
        this.courierEventProducer = courierEventProducer;
    }

    @KafkaListener(topics = "delivery.events")
    public void handleDeliveryEvent(DeliveryEvent event) {

        if (event.eventType() == DeliveryEventType.DELIVERY_CREATED) {

            CourierResponse courier =
                    courierService.assignDelivery(event.deliveryId());

            courierEventProducer.sendCourierAssigned(
                    event.deliveryId(),
                    courier.id(),
                    courier.userId()
            );
        }

        if (event.eventType() == DeliveryEventType.DELIVERY_COMPLETED) {
            courierService.releaseCourier(event.deliveryId());
        }

        if (event.eventType() == DeliveryEventType.DELIVERY_CANCELLED) {
            courierService.releaseCourierIfAssigned(
                    event.deliveryId()
            );
        }
    }
}