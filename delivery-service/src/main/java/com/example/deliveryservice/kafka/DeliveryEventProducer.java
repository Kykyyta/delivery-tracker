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

    public void sendDeliveryCreated(Long deliveryId) {
        sendEvent(
                DeliveryEventType.DELIVERY_CREATED,
                deliveryId
        );
    }

    public void sendDeliveryPickedUp(Long deliveryId) {
        sendEvent(
                DeliveryEventType.DELIVERY_PICKED_UP,
                deliveryId
        );
    }

    public void sendDeliveryCompleted(Long deliveryId) {
        sendEvent(
                DeliveryEventType.DELIVERY_COMPLETED,
                deliveryId
        );
    }

    private void sendEvent(
            DeliveryEventType eventType,
            Long deliveryId
    ) {
        DeliveryEvent event = new DeliveryEvent(
                UUID.randomUUID(),
                eventType,
                deliveryId,
                LocalDateTime.now()
        );

        kafkaTemplate.send(
                TOPIC,
                deliveryId.toString(),
                event
        );
    }
}