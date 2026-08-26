package com.example.deliveryservice.service;

import com.example.deliveryservice.dto.DeliveryRequest;
import com.example.deliveryservice.dto.DeliveryResponse;
import com.example.deliveryservice.exception.DeliveryNotFoundException;
import com.example.deliveryservice.exception.InvalidDeliveryStatusException;
import com.example.deliveryservice.kafka.DeliveryEventProducer;
import com.example.deliveryservice.mapper.DeliveryMapper;
import com.example.deliveryservice.model.Delivery;
import com.example.deliveryservice.model.DeliveryStatus;
import com.example.deliveryservice.repository.DeliveryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryEventProducer deliveryEventProducer;

    public DeliveryService(
            DeliveryRepository deliveryRepository,
            DeliveryMapper deliveryMapper,
            DeliveryEventProducer deliveryEventProducer
    ) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryMapper = deliveryMapper;
        this.deliveryEventProducer = deliveryEventProducer;
    }

    @Transactional
    public DeliveryResponse createDelivery(
            DeliveryRequest request,
            Long customerId
    ) {
        Delivery delivery =
                deliveryMapper.toEntity(request);

        delivery.setCustomerId(customerId);

        Delivery savedDelivery =
                deliveryRepository.save(delivery);

        deliveryEventProducer.sendDeliveryCreated(
                savedDelivery.getId(),
                savedDelivery.getCustomerId()
        );

        return deliveryMapper.toResponse(
                savedDelivery
        );
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getAllDeliveries(
            DeliveryStatus status,
            Long courierId,
            Long currentUserId,
            String role
    ) {
        List<Delivery> deliveries;

        if ("CUSTOMER".equals(role)) {

            if (status != null) {
                deliveries =
                        deliveryRepository
                                .findByCustomerIdAndStatus(
                                        currentUserId,
                                        status
                                );
            } else {
                deliveries =
                        deliveryRepository
                                .findByCustomerId(
                                        currentUserId
                                );
            }

            return deliveries.stream()
                    .map(deliveryMapper::toResponse)
                    .toList();
        }

        if ("COURIER".equals(role)) {

            if (status != null) {
                deliveries =
                        deliveryRepository
                                .findByCourierUserIdAndStatus(
                                        currentUserId,
                                        status
                                );
            } else {
                deliveries =
                        deliveryRepository
                                .findByCourierUserId(
                                        currentUserId
                                );
            }

            return deliveries.stream()
                    .map(deliveryMapper::toResponse)
                    .toList();
        }

        if ("ADMIN".equals(role)) {

            if (status != null && courierId != null) {
                deliveries =
                        deliveryRepository
                                .findByStatusAndCourierId(
                                        status,
                                        courierId
                                );

            } else if (status != null) {
                deliveries =
                        deliveryRepository.findByStatus(
                                status
                        );

            } else if (courierId != null) {
                deliveries =
                        deliveryRepository.findByCourierId(
                                courierId
                        );

            } else {
                deliveries =
                        deliveryRepository.findAll();
            }

            return deliveries.stream()
                    .map(deliveryMapper::toResponse)
                    .toList();
        }

        throw new AccessDeniedException(
                "Нет доступа к доставкам"
        );
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryById(
            Long id,
            Long currentUserId,
            String role
    ) {
        Delivery delivery =
                findDeliveryById(id);

        checkReadAccess(
                delivery,
                currentUserId,
                role
        );

        return deliveryMapper.toResponse(
                delivery
        );
    }

    @Transactional
    public DeliveryResponse updateDelivery(
            Long id,
            DeliveryRequest request,
            Long currentUserId,
            String role
    ) {
        Delivery delivery =
                findDeliveryById(id);

        checkCustomerAccess(
                delivery,
                currentUserId,
                role
        );

        deliveryMapper.updateEntity(
                delivery,
                request
        );

        Delivery updatedDelivery =
                deliveryRepository.save(delivery);

        return deliveryMapper.toResponse(
                updatedDelivery
        );
    }

    @Transactional
    public DeliveryResponse assignCourier(
            Long id,
            Long courierId,
            Long courierUserId
    ) {
        Delivery delivery =
                findDeliveryById(id);

        if (delivery.getStatus()
                != DeliveryStatus.CREATED) {

            throw new InvalidDeliveryStatusException(
                    "Курьера можно назначить только доставке со статусом CREATED"
            );
        }

        delivery.setCourierId(courierId);
        delivery.setCourierUserId(courierUserId);
        delivery.setStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        Delivery updatedDelivery =
                deliveryRepository.save(delivery);

        return deliveryMapper.toResponse(
                updatedDelivery
        );
    }

    @Transactional
    public DeliveryResponse pickupDelivery(
            Long id,
            Long currentUserId,
            String role
    ) {
        Delivery delivery =
                findDeliveryById(id);

        checkCourierAccess(
                delivery,
                currentUserId,
                role
        );

        if (delivery.getStatus()
                != DeliveryStatus.COURIER_ASSIGNED) {

            throw new InvalidDeliveryStatusException(
                    "Забрать можно только доставку со статусом COURIER_ASSIGNED"
            );
        }

        delivery.setStatus(
                DeliveryStatus.PICKED_UP
        );

        Delivery updatedDelivery =
                deliveryRepository.save(delivery);

        deliveryEventProducer.sendDeliveryPickedUp(
                updatedDelivery.getId(),
                updatedDelivery.getCustomerId()
        );

        return deliveryMapper.toResponse(
                updatedDelivery
        );
    }

    @Transactional
    public DeliveryResponse completeDelivery(
            Long id,
            Long currentUserId,
            String role
    ) {
        Delivery delivery =
                findDeliveryById(id);

        checkCourierAccess(
                delivery,
                currentUserId,
                role
        );

        if (delivery.getStatus()
                != DeliveryStatus.PICKED_UP) {

            throw new InvalidDeliveryStatusException(
                    "Завершить можно только доставку со статусом PICKED_UP"
            );
        }

        delivery.setStatus(
                DeliveryStatus.COMPLETED
        );

        Delivery updatedDelivery =
                deliveryRepository.save(delivery);

        deliveryEventProducer.sendDeliveryCompleted(
                updatedDelivery.getId(),
                updatedDelivery.getCustomerId()
        );

        return deliveryMapper.toResponse(
                updatedDelivery
        );
    }

    @Transactional
    public DeliveryResponse cancelDelivery(
            Long id,
            Long currentUserId,
            String role
    ) {
        Delivery delivery =
                findDeliveryById(id);

        checkCustomerAccess(
                delivery,
                currentUserId,
                role
        );

        if (delivery.getStatus()
                != DeliveryStatus.CREATED
                && delivery.getStatus()
                != DeliveryStatus.COURIER_ASSIGNED) {

            throw new InvalidDeliveryStatusException(
                    "Отменить можно только доставку со статусом CREATED или COURIER_ASSIGNED"
            );
        }

        delivery.setStatus(
                DeliveryStatus.CANCELLED
        );

        Delivery updatedDelivery =
                deliveryRepository.save(delivery);

        deliveryEventProducer.sendDeliveryCancelled(
                updatedDelivery.getId(),
                updatedDelivery.getCustomerId()
        );

        return deliveryMapper.toResponse(
                updatedDelivery
        );
    }

    @Transactional
    public void deleteDelivery(Long id) {
        Delivery delivery =
                findDeliveryById(id);

        if (delivery.getStatus() != DeliveryStatus.CANCELLED
                && delivery.getStatus() != DeliveryStatus.COMPLETED) {

            throw new InvalidDeliveryStatusException(
                    "Удалить можно только отменённую или завершённую доставку"
            );
        }

        deliveryRepository.delete(delivery);
    }

    private Delivery findDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() ->
                        new DeliveryNotFoundException(id)
                );
    }

    private void checkReadAccess(
            Delivery delivery,
            Long currentUserId,
            String role
    ) {
        if ("ADMIN".equals(role)) {
            return;
        }

        if ("CUSTOMER".equals(role)
                && Objects.equals(
                delivery.getCustomerId(),
                currentUserId
        )) {
            return;
        }

        if ("COURIER".equals(role)
                && Objects.equals(
                delivery.getCourierUserId(),
                currentUserId
        )) {
            return;
        }

        throw new AccessDeniedException(
                "Нет доступа к этой доставке"
        );
    }

    private void checkCustomerAccess(
            Delivery delivery,
            Long currentUserId,
            String role
    ) {
        if ("ADMIN".equals(role)) {
            return;
        }

        if ("CUSTOMER".equals(role)
                && Objects.equals(
                delivery.getCustomerId(),
                currentUserId
        )) {
            return;
        }

        throw new AccessDeniedException(
                "Нет доступа к этой доставке"
        );
    }

    private void checkCourierAccess(
            Delivery delivery,
            Long currentUserId,
            String role
    ) {
        if ("ADMIN".equals(role)) {
            return;
        }

        if ("COURIER".equals(role)
                && Objects.equals(
                delivery.getCourierUserId(),
                currentUserId
        )) {
            return;
        }

        throw new AccessDeniedException(
                "Эта доставка не назначена текущему курьеру"
        );
    }
}