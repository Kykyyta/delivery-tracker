package com.example.deliveryservice.kafka;

import com.example.deliveryservice.event.DeliveryEvent;
import com.example.deliveryservice.event.DeliveryEventType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DeliveryEventProducer {

    private static final String TOPIC = "delivery.events";

    private final KafkaTemplate<String, DeliveryEvent> kafkaTemplate;

    public DeliveryEventProducer(
            KafkaTemplate<String, DeliveryEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendDeliveryCreated(
            Long deliveryId,
            Long customerId
    ) {
        sendEvent(
                DeliveryEventType.DELIVERY_CREATED,
                deliveryId,
                customerId
        );
    }

    public void sendDeliveryPickedUp(
            Long deliveryId,
            Long customerId
    ) {
        sendEvent(
                DeliveryEventType.DELIVERY_PICKED_UP,
                deliveryId,
                customerId
        );
    }

    public void sendDeliveryCompleted(
            Long deliveryId,
            Long customerId
    ) {
        sendEvent(
                DeliveryEventType.DELIVERY_COMPLETED,
                deliveryId,
                customerId
        );
    }

    public void sendDeliveryCancelled(
            Long deliveryId,
            Long customerId
    ) {
        sendEvent(
                DeliveryEventType.DELIVERY_CANCELLED,
                deliveryId,
                customerId
        );
    }

    private void sendEvent(
            DeliveryEventType eventType,
            Long deliveryId,
            Long customerId
    ) {
        DeliveryEvent event = new DeliveryEvent(
                UUID.randomUUID(),
                eventType,
                deliveryId,
                customerId,
                LocalDateTime.now()
        );

        kafkaTemplate.send(
                TOPIC,
                deliveryId.toString(),
                event
        );
    }
}