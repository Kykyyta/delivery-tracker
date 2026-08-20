package com.example.notificationservice.config;

import com.example.notificationservice.event.CourierEvent;
import com.example.notificationservice.event.DeliveryEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, DeliveryEvent> deliveryEventConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> properties =
                kafkaProperties.buildConsumerProperties();

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                new JacksonJsonDeserializer<>(
                        DeliveryEvent.class,
                        false
                )
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryEvent>
    deliveryEventKafkaListenerContainerFactory(
            ConsumerFactory<String, DeliveryEvent> deliveryEventConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(deliveryEventConsumerFactory);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, CourierEvent> courierEventConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> properties =
                kafkaProperties.buildConsumerProperties();

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                new JacksonJsonDeserializer<>(
                        CourierEvent.class,
                        false
                )
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CourierEvent>
    courierEventKafkaListenerContainerFactory(
            ConsumerFactory<String, CourierEvent> courierEventConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, CourierEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(courierEventConsumerFactory);

        return factory;
    }
}