package com.example.deliveryservice.mapper;

import com.example.deliveryservice.dto.DeliveryRequest;
import com.example.deliveryservice.dto.DeliveryResponse;
import com.example.deliveryservice.model.Delivery;
import org.springframework.stereotype.Component;

@Component
public class DeliveryMapper {

    public Delivery toEntity(DeliveryRequest request) {
        Delivery delivery = new Delivery();

        delivery.setCustomerName(request.customerName());
        delivery.setCustomerPhone(request.customerPhone());
        delivery.setPickupAddress(request.pickupAddress());
        delivery.setDeliveryAddress(request.deliveryAddress());

        return delivery;
    }

    public void updateEntity(Delivery delivery, DeliveryRequest request) {
        delivery.setCustomerName(request.customerName());
        delivery.setCustomerPhone(request.customerPhone());
        delivery.setPickupAddress(request.pickupAddress());
        delivery.setDeliveryAddress(request.deliveryAddress());
    }

    public DeliveryResponse toResponse(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getCustomerId(),
                delivery.getCustomerName(),
                delivery.getCustomerPhone(),
                delivery.getPickupAddress(),
                delivery.getDeliveryAddress(),
                delivery.getStatus(),
                delivery.getCourierId(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}