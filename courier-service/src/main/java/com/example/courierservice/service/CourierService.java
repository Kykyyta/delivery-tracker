package com.example.courierservice.service;

import com.example.courierservice.dto.CourierRequest;
import com.example.courierservice.dto.CourierResponse;
import com.example.courierservice.exception.CourierNotFoundException;
import com.example.courierservice.exception.DuplicateCourierPhoneException;
import com.example.courierservice.exception.InvalidCourierStatusException;
import com.example.courierservice.exception.NoAvailableCourierException;
import com.example.courierservice.mapper.CourierMapper;
import com.example.courierservice.model.Courier;
import com.example.courierservice.model.CourierStatus;
import com.example.courierservice.repository.CourierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourierService {

    private final CourierRepository courierRepository;
    private final CourierMapper courierMapper;

    public CourierService(
            CourierRepository courierRepository,
            CourierMapper courierMapper
    ) {
        this.courierRepository = courierRepository;
        this.courierMapper = courierMapper;
    }

    @Transactional
    public CourierResponse createCourier(CourierRequest request) {

        if (courierRepository.existsByPhone(request.phone())) {
            throw new DuplicateCourierPhoneException(request.phone());
        }

        Courier courier = courierMapper.toEntity(request);

        Courier savedCourier = courierRepository.save(courier);

        return courierMapper.toResponse(savedCourier);
    }

    @Transactional(readOnly = true)
    public List<CourierResponse> getAllCouriers(CourierStatus status) {

        List<Courier> couriers;

        if (status != null) {
            couriers = courierRepository.findByStatus(status);
        } else {
            couriers = courierRepository.findAll();
        }

        return couriers.stream()
                .map(courierMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourierResponse getCourierById(Long id) {

        Courier courier = findCourierById(id);

        return courierMapper.toResponse(courier);
    }

    @Transactional
    public CourierResponse updateCourier(
            Long id,
            CourierRequest request
    ) {
        Courier courier = findCourierById(id);

        if (courierRepository.existsByPhoneAndIdNot(
                request.phone(),
                id
        )) {
            throw new DuplicateCourierPhoneException(request.phone());
        }

        courierMapper.updateEntity(courier, request);

        Courier updatedCourier = courierRepository.save(courier);

        return courierMapper.toResponse(updatedCourier);
    }

    @Transactional
    public void deleteCourier(Long id) {

        Courier courier = findCourierById(id);

        if (courier.getStatus() == CourierStatus.BUSY) {
            throw new InvalidCourierStatusException(
                    "Нельзя удалить курьера, который выполняет доставку"
            );
        }

        courierRepository.delete(courier);
    }

    @Transactional
    public CourierResponse goOffline(Long id) {

        Courier courier = findCourierById(id);

        if (courier.getStatus() == CourierStatus.BUSY) {
            throw new InvalidCourierStatusException(
                    "Нельзя перевести занятого курьера в статус OFFLINE"
            );
        }

        courier.setStatus(CourierStatus.OFFLINE);

        Courier updatedCourier = courierRepository.save(courier);

        return courierMapper.toResponse(updatedCourier);
    }

    @Transactional
    public CourierResponse goOnline(Long id) {

        Courier courier = findCourierById(id);

        if (courier.getStatus() != CourierStatus.OFFLINE) {
            throw new InvalidCourierStatusException(
                    "В статус AVAILABLE можно вернуть только курьера со статусом OFFLINE"
            );
        }

        courier.setStatus(CourierStatus.AVAILABLE);

        Courier updatedCourier = courierRepository.save(courier);

        return courierMapper.toResponse(updatedCourier);
    }

    @Transactional
    public CourierResponse assignDelivery(Long deliveryId) {

        Courier courier = courierRepository
                .findFirstByStatus(CourierStatus.AVAILABLE)
                .orElseThrow(NoAvailableCourierException::new);

        courier.setStatus(CourierStatus.BUSY);
        courier.setCurrentDeliveryId(deliveryId);

        Courier updatedCourier = courierRepository.save(courier);

        return courierMapper.toResponse(updatedCourier);
    }

    @Transactional
    public CourierResponse releaseCourier(Long deliveryId) {

        Courier courier = courierRepository
                .findByCurrentDeliveryId(deliveryId)
                .orElseThrow(() -> new InvalidCourierStatusException(
                        "Курьер для доставки с id " + deliveryId + " не найден"
                ));

        if (courier.getStatus() != CourierStatus.BUSY) {
            throw new InvalidCourierStatusException(
                    "Освободить можно только курьера со статусом BUSY"
            );
        }

        courier.setStatus(CourierStatus.AVAILABLE);
        courier.setCurrentDeliveryId(null);

        Courier updatedCourier = courierRepository.save(courier);

        return courierMapper.toResponse(updatedCourier);
    }

    private Courier findCourierById(Long id) {
        return courierRepository.findById(id)
                .orElseThrow(() -> new CourierNotFoundException(id));
    }
}
