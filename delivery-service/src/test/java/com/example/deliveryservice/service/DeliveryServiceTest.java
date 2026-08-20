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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private DeliveryEventProducer deliveryEventProducer;

    @InjectMocks
    private DeliveryService deliveryService;

    private Delivery delivery;
    private DeliveryRequest request;
    private DeliveryResponse response;

    @BeforeEach
    void setUp() {
        request = new DeliveryRequest(
                "Иван Иванов",
                "+79991234567",
                "Москва, Тверская 10",
                "Москва, Арбат 15"
        );

        delivery = new Delivery();
        delivery.setId(1L);
        delivery.setCustomerName("Иван Иванов");
        delivery.setCustomerPhone("+79991234567");
        delivery.setPickupAddress("Москва, Тверская 10");
        delivery.setDeliveryAddress("Москва, Арбат 15");
        delivery.setStatus(DeliveryStatus.CREATED);
        delivery.setCreatedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());

        response = new DeliveryResponse(
                delivery.getId(),
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

    @Test
    void createDeliveryShouldReturnCreatedDelivery() {
        when(deliveryMapper.toEntity(request)).thenReturn(delivery);
        when(deliveryRepository.save(delivery)).thenReturn(delivery);
        when(deliveryMapper.toResponse(delivery)).thenReturn(response);

        DeliveryResponse result = deliveryService.createDelivery(request);

        assertEquals(response, result);

        verify(deliveryMapper).toEntity(request);
        verify(deliveryRepository).save(delivery);
        verify(deliveryMapper).toResponse(delivery);
    }

    @Test
    void getAllDeliveriesShouldReturnAllDeliveries() {
        when(deliveryRepository.findAll()).thenReturn(List.of(delivery));
        when(deliveryMapper.toResponse(delivery)).thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(null, null);

        assertEquals(1, result.size());
        assertEquals(response, result.getFirst());

        verify(deliveryRepository).findAll();
    }

    @Test
    void getAllDeliveriesShouldFilterByStatus() {
        when(deliveryRepository.findByStatus(DeliveryStatus.CREATED))
                .thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        DeliveryStatus.CREATED,
                        null
                );

        assertEquals(1, result.size());
        assertEquals(response, result.getFirst());

        verify(deliveryRepository)
                .findByStatus(DeliveryStatus.CREATED);

        verify(deliveryRepository, never()).findAll();
    }

    @Test
    void getAllDeliveriesShouldFilterByCourierId() {
        delivery.setCourierId(5L);

        when(deliveryRepository.findByCourierId(5L))
                .thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(null, 5L);

        assertEquals(1, result.size());

        verify(deliveryRepository).findByCourierId(5L);
    }

    @Test
    void getAllDeliveriesShouldFilterByStatusAndCourierId() {
        delivery.setStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setCourierId(5L);

        when(deliveryRepository.findByStatusAndCourierId(
                DeliveryStatus.COURIER_ASSIGNED,
                5L
        )).thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        DeliveryStatus.COURIER_ASSIGNED,
                        5L
                );

        assertEquals(1, result.size());

        verify(deliveryRepository).findByStatusAndCourierId(
                DeliveryStatus.COURIER_ASSIGNED,
                5L
        );
    }

    @Test
    void getDeliveryByIdShouldReturnDelivery() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.getDeliveryById(1L);

        assertEquals(response, result);

        verify(deliveryRepository).findById(1L);
    }

    @Test
    void getDeliveryByIdShouldThrowExceptionWhenDeliveryNotFound() {
        when(deliveryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                DeliveryNotFoundException.class,
                () -> deliveryService.getDeliveryById(999L)
        );

        verify(deliveryRepository).findById(999L);
        verify(deliveryMapper, never()).toResponse(any());
    }

    @Test
    void updateDeliveryShouldReturnUpdatedDelivery() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.updateDelivery(1L, request);

        assertEquals(response, result);

        verify(deliveryMapper).updateEntity(delivery, request);
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void deleteDeliveryShouldDeleteDelivery() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        deliveryService.deleteDelivery(1L);

        verify(deliveryRepository).delete(delivery);
    }

    @Test
    void assignCourierShouldAssignCourier() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenAnswer(invocation -> new DeliveryResponse(
                        delivery.getId(),
                        delivery.getCustomerName(),
                        delivery.getCustomerPhone(),
                        delivery.getPickupAddress(),
                        delivery.getDeliveryAddress(),
                        delivery.getStatus(),
                        delivery.getCourierId(),
                        delivery.getCreatedAt(),
                        delivery.getUpdatedAt()
                ));

        DeliveryResponse result =
                deliveryService.assignCourier(1L, 5L);

        assertEquals(DeliveryStatus.COURIER_ASSIGNED, result.status());
        assertEquals(5L, result.courierId());

        assertEquals(
                DeliveryStatus.COURIER_ASSIGNED,
                delivery.getStatus()
        );

        assertEquals(5L, delivery.getCourierId());

        verify(deliveryRepository).save(delivery);
    }

    @Test
    void assignCourierShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(DeliveryStatus.PICKED_UP);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.assignCourier(1L, 5L)
        );

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void pickupDeliveryShouldChangeStatusToPickedUp() {
        delivery.setStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setCourierId(5L);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        deliveryService.pickupDelivery(1L);

        assertEquals(
                DeliveryStatus.PICKED_UP,
                delivery.getStatus()
        );

        verify(deliveryRepository).save(delivery);
    }

    @Test
    void pickupDeliveryShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(DeliveryStatus.CREATED);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.pickupDelivery(1L)
        );

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void completeDeliveryShouldChangeStatusToCompleted() {
        delivery.setStatus(DeliveryStatus.PICKED_UP);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        deliveryService.completeDelivery(1L);

        assertEquals(
                DeliveryStatus.COMPLETED,
                delivery.getStatus()
        );

        verify(deliveryRepository).save(delivery);
    }

    @Test
    void completeDeliveryShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(DeliveryStatus.CREATED);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.completeDelivery(1L)
        );

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void cancelDeliveryShouldChangeStatusToCancelled() {
        delivery.setStatus(DeliveryStatus.CREATED);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        deliveryService.cancelDelivery(1L);

        assertEquals(
                DeliveryStatus.CANCELLED,
                delivery.getStatus()
        );

        verify(deliveryRepository).save(delivery);
    }

    @Test
    void cancelDeliveryShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(DeliveryStatus.COMPLETED);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.cancelDelivery(1L)
        );

        verify(deliveryRepository, never()).save(any());
    }
}
