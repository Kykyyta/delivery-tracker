package com.example.courierservice.mapper;

import com.example.courierservice.dto.CourierCreateRequest;
import com.example.courierservice.dto.CourierRequest;
import com.example.courierservice.dto.CourierResponse;
import com.example.courierservice.model.Courier;
import org.springframework.stereotype.Component;

@Component
public class CourierMapper {

    public Courier toEntity(CourierCreateRequest request) {
        Courier courier = new Courier();

        courier.setUserId(request.userId());
        courier.setName(request.name());
        courier.setPhone(request.phone());

        return courier;
    }

    public void updateEntity(
            Courier courier,
            CourierRequest request
    ) {
        courier.setName(request.name());
        courier.setPhone(request.phone());
    }

    public CourierResponse toResponse(Courier courier) {
        return new CourierResponse(
                courier.getId(),
                courier.getUserId(),
                courier.getName(),
                courier.getPhone(),
                courier.getStatus(),
                courier.getCurrentDeliveryId(),
                courier.getCreatedAt(),
                courier.getUpdatedAt()
        );
    }
}