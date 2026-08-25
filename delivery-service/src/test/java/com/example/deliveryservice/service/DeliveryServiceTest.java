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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    private static final Long CUSTOMER_ID = 10L;
    private static final Long OTHER_CUSTOMER_ID = 11L;
    private static final Long COURIER_ID = 5L;
    private static final Long COURIER_USER_ID = 20L;
    private static final Long OTHER_COURIER_USER_ID = 21L;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private DeliveryEventProducer deliveryEventProducer;

    @Mock
    private DeliveryResponse response;

    @InjectMocks
    private DeliveryService deliveryService;

    private Delivery delivery;
    private DeliveryRequest request;

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
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setCustomerName("Иван Иванов");
        delivery.setCustomerPhone("+79991234567");
        delivery.setPickupAddress("Москва, Тверская 10");
        delivery.setDeliveryAddress("Москва, Арбат 15");
        delivery.setStatus(DeliveryStatus.CREATED);
        delivery.setCreatedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createDeliveryShouldSetCustomerIdAndReturnCreatedDelivery() {
        delivery.setCustomerId(null);

        when(deliveryMapper.toEntity(request))
                .thenReturn(delivery);

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.createDelivery(
                        request,
                        CUSTOMER_ID
                );

        assertSame(response, result);
        assertEquals(CUSTOMER_ID, delivery.getCustomerId());

        verify(deliveryMapper).toEntity(request);
        verify(deliveryRepository).save(delivery);
        verify(deliveryMapper).toResponse(delivery);

        verify(deliveryEventProducer)
                .sendDeliveryCreated(delivery.getId());
    }

    @Test
    void getAllDeliveriesShouldReturnAllDeliveriesForAdmin() {
        when(deliveryRepository.findAll())
                .thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        null,
                        null,
                        1L,
                        "ADMIN"
                );

        assertEquals(1, result.size());
        assertSame(response, result.getFirst());

        verify(deliveryRepository).findAll();
    }

    @Test
    void getAllDeliveriesShouldFilterByStatusForAdmin() {
        when(deliveryRepository.findByStatus(
                DeliveryStatus.CREATED
        )).thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        DeliveryStatus.CREATED,
                        null,
                        1L,
                        "ADMIN"
                );

        assertEquals(1, result.size());
        assertSame(response, result.getFirst());

        verify(deliveryRepository)
                .findByStatus(DeliveryStatus.CREATED);

        verify(deliveryRepository, never())
                .findAll();
    }

    @Test
    void getAllDeliveriesShouldFilterByCourierIdForAdmin() {
        delivery.setCourierId(COURIER_ID);

        when(deliveryRepository.findByCourierId(COURIER_ID))
                .thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        null,
                        COURIER_ID,
                        1L,
                        "ADMIN"
                );

        assertEquals(1, result.size());
        assertSame(response, result.getFirst());

        verify(deliveryRepository)
                .findByCourierId(COURIER_ID);
    }

    @Test
    void getAllDeliveriesShouldFilterByStatusAndCourierIdForAdmin() {
        delivery.setStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setCourierId(COURIER_ID);

        when(deliveryRepository.findByStatusAndCourierId(
                DeliveryStatus.COURIER_ASSIGNED,
                COURIER_ID
        )).thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        DeliveryStatus.COURIER_ASSIGNED,
                        COURIER_ID,
                        1L,
                        "ADMIN"
                );

        assertEquals(1, result.size());
        assertSame(response, result.getFirst());

        verify(deliveryRepository)
                .findByStatusAndCourierId(
                        DeliveryStatus.COURIER_ASSIGNED,
                        COURIER_ID
                );
    }

    @Test
    void getAllDeliveriesShouldReturnOnlyCustomerDeliveries() {
        when(deliveryRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        null,
                        null,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertEquals(1, result.size());
        assertSame(response, result.getFirst());

        verify(deliveryRepository)
                .findByCustomerId(CUSTOMER_ID);

        verify(deliveryRepository, never())
                .findAll();
    }

    @Test
    void getAllDeliveriesShouldReturnOnlyCustomerDeliveriesWithStatus() {
        when(deliveryRepository.findByCustomerIdAndStatus(
                CUSTOMER_ID,
                DeliveryStatus.CREATED
        )).thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        DeliveryStatus.CREATED,
                        null,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertEquals(1, result.size());
        assertSame(response, result.getFirst());

        verify(deliveryRepository)
                .findByCustomerIdAndStatus(
                        CUSTOMER_ID,
                        DeliveryStatus.CREATED
                );
    }

    @Test
    void getDeliveryByIdShouldReturnDeliveryForOwnerCustomer() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.getDeliveryById(
                        1L,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertSame(response, result);

        verify(deliveryRepository).findById(1L);
        verify(deliveryMapper).toResponse(delivery);
    }

    @Test
    void getDeliveryByIdShouldThrowAccessDeniedForAnotherCustomer() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.getDeliveryById(
                        1L,
                        OTHER_CUSTOMER_ID,
                        "CUSTOMER"
                )
        );

        verify(deliveryMapper, never())
                .toResponse(any());
    }

    @Test
    void getDeliveryByIdShouldReturnDeliveryForAdmin() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.getDeliveryById(
                        1L,
                        1L,
                        "ADMIN"
                );

        assertSame(response, result);
    }

    @Test
    void getDeliveryByIdShouldThrowExceptionWhenDeliveryNotFound() {
        when(deliveryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                DeliveryNotFoundException.class,
                () -> deliveryService.getDeliveryById(
                        999L,
                        CUSTOMER_ID,
                        "CUSTOMER"
                )
        );

        verify(deliveryRepository).findById(999L);

        verify(deliveryMapper, never())
                .toResponse(any());
    }

    @Test
    void updateDeliveryShouldUpdateOwnDeliveryForCustomer() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.updateDelivery(
                        1L,
                        request,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertSame(response, result);

        verify(deliveryMapper)
                .updateEntity(delivery, request);

        verify(deliveryRepository).save(delivery);
    }

    @Test
    void updateDeliveryShouldThrowAccessDeniedForAnotherCustomer() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.updateDelivery(
                        1L,
                        request,
                        OTHER_CUSTOMER_ID,
                        "CUSTOMER"
                )
        );

        verify(deliveryMapper, never())
                .updateEntity(any(), any());

        verify(deliveryRepository, never())
                .save(any());
    }

    @Test
    void deleteDeliveryShouldDeleteDelivery() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        deliveryService.deleteDelivery(1L);

        verify(deliveryRepository)
                .delete(delivery);
    }

    @Test
    void assignCourierShouldAssignCourierAndCourierUser() {
        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.assignCourier(
                        1L,
                        COURIER_ID,
                        COURIER_USER_ID
                );

        assertSame(response, result);

        assertEquals(
                DeliveryStatus.COURIER_ASSIGNED,
                delivery.getStatus()
        );

        assertEquals(
                COURIER_ID,
                delivery.getCourierId()
        );

        assertEquals(
                COURIER_USER_ID,
                delivery.getCourierUserId()
        );

        verify(deliveryRepository)
                .save(delivery);
    }

    @Test
    void assignCourierShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(DeliveryStatus.PICKED_UP);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.assignCourier(
                        1L,
                        COURIER_ID,
                        COURIER_USER_ID
                )
        );

        verify(deliveryRepository, never())
                .save(any());
    }

    @Test
    void pickupDeliveryShouldChangeStatusForAssignedCourier() {
        delivery.setStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setCourierId(COURIER_ID);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.pickupDelivery(
                        1L,
                        COURIER_USER_ID,
                        "COURIER"
                );

        assertSame(response, result);

        assertEquals(
                DeliveryStatus.PICKED_UP,
                delivery.getStatus()
        );

        verify(deliveryRepository).save(delivery);

        verify(deliveryEventProducer)
                .sendDeliveryPickedUp(1L);
    }

    @Test
    void pickupDeliveryShouldThrowAccessDeniedForAnotherCourier() {
        delivery.setStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setCourierId(COURIER_ID);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.pickupDelivery(
                        1L,
                        OTHER_COURIER_USER_ID,
                        "COURIER"
                )
        );

        assertEquals(
                DeliveryStatus.COURIER_ASSIGNED,
                delivery.getStatus()
        );

        verify(deliveryRepository, never())
                .save(any());

        verify(deliveryEventProducer, never())
                .sendDeliveryPickedUp(anyLong());
    }

    @Test
    void pickupDeliveryShouldAllowAdmin() {
        delivery.setStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        deliveryService.pickupDelivery(
                1L,
                999L,
                "ADMIN"
        );

        assertEquals(
                DeliveryStatus.PICKED_UP,
                delivery.getStatus()
        );
    }

    @Test
    void pickupDeliveryShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(DeliveryStatus.CREATED);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.pickupDelivery(
                        1L,
                        COURIER_USER_ID,
                        "COURIER"
                )
        );

        verify(deliveryRepository, never())
                .save(any());
    }

    @Test
    void completeDeliveryShouldChangeStatusForAssignedCourier() {
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        delivery.setCourierId(COURIER_ID);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.completeDelivery(
                        1L,
                        COURIER_USER_ID,
                        "COURIER"
                );

        assertSame(response, result);

        assertEquals(
                DeliveryStatus.COMPLETED,
                delivery.getStatus()
        );

        verify(deliveryRepository).save(delivery);

        verify(deliveryEventProducer)
                .sendDeliveryCompleted(1L);
    }

    @Test
    void completeDeliveryShouldThrowAccessDeniedForAnotherCourier() {
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        delivery.setCourierId(COURIER_ID);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.completeDelivery(
                        1L,
                        OTHER_COURIER_USER_ID,
                        "COURIER"
                )
        );

        assertEquals(
                DeliveryStatus.PICKED_UP,
                delivery.getStatus()
        );

        verify(deliveryRepository, never())
                .save(any());

        verify(deliveryEventProducer, never())
                .sendDeliveryCompleted(anyLong());
    }

    @Test
    void completeDeliveryShouldAllowAdmin() {
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        deliveryService.completeDelivery(
                1L,
                999L,
                "ADMIN"
        );

        assertEquals(
                DeliveryStatus.COMPLETED,
                delivery.getStatus()
        );
    }

    @Test
    void completeDeliveryShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(DeliveryStatus.CREATED);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.completeDelivery(
                        1L,
                        COURIER_USER_ID,
                        "COURIER"
                )
        );

        verify(deliveryRepository, never())
                .save(any());
    }

    @Test
    void cancelDeliveryShouldCancelOwnDeliveryForCustomer() {
        delivery.setStatus(DeliveryStatus.CREATED);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(response);

        DeliveryResponse result =
                deliveryService.cancelDelivery(
                        1L,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertSame(response, result);

        assertEquals(
                DeliveryStatus.CANCELLED,
                delivery.getStatus()
        );

        verify(deliveryRepository).save(delivery);
    }

    @Test
    void cancelDeliveryShouldThrowAccessDeniedForAnotherCustomer() {
        delivery.setStatus(DeliveryStatus.CREATED);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.cancelDelivery(
                        1L,
                        OTHER_CUSTOMER_ID,
                        "CUSTOMER"
                )
        );

        assertEquals(
                DeliveryStatus.CREATED,
                delivery.getStatus()
        );

        verify(deliveryRepository, never())
                .save(any());
    }

    @Test
    void cancelDeliveryShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(DeliveryStatus.COMPLETED);

        when(deliveryRepository.findById(1L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.cancelDelivery(
                        1L,
                        CUSTOMER_ID,
                        "CUSTOMER"
                )
        );

        verify(deliveryRepository, never())
                .save(any());
    }
}