package com.example.courierservice.kafka;

import com.example.courierservice.event.CourierEvent;
import com.example.courierservice.event.CourierEventType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class CourierEventProducer {

    private static final String TOPIC = "courier.events";

    private final KafkaTemplate<String, CourierEvent> kafkaTemplate;

    public CourierEventProducer(
            KafkaTemplate<String, CourierEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCourierAssigned(
            Long deliveryId,
            Long courierId,
            Long courierUserId
    ) {
        CourierEvent event = new CourierEvent(
                UUID.randomUUID(),
                CourierEventType.COURIER_ASSIGNED,
                deliveryId,
                courierId,
                courierUserId,
                LocalDateTime.now()
        );

        kafkaTemplate.send(
                TOPIC,
                deliveryId.toString(),
                event
        );
    }
}