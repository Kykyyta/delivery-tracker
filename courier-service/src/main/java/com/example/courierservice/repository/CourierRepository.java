package com.example.courierservice.repository;

import com.example.courierservice.model.Courier;
import com.example.courierservice.model.CourierStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourierRepository extends JpaRepository<Courier, Long> {

    List<Courier> findByStatus(CourierStatus status);

    Optional<Courier> findFirstByStatus(CourierStatus status);

    Optional<Courier> findByCurrentDeliveryId(Long deliveryId);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}