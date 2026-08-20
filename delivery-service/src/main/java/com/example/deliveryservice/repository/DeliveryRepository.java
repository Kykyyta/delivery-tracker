package com.example.deliveryservice.repository;

import com.example.deliveryservice.model.Delivery;
import com.example.deliveryservice.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByStatus(DeliveryStatus status);

    List<Delivery> findByCourierId(Long courierId);

    List<Delivery> findByStatusAndCourierId(
            DeliveryStatus status,
            Long courierId
    );
}
