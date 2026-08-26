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

    private static final Long DELIVERY_ID = 1L;

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
    private DeliveryResponse deliveryResponse;

    @InjectMocks
    private DeliveryService deliveryService;

    private Delivery delivery;
    private DeliveryRequest deliveryRequest;

    @BeforeEach
    void setUp() {
        deliveryRequest = new DeliveryRequest(
                "Иван Иванов",
                "+79991234567",
                "Москва, Тверская 10",
                "Москва, Арбат 15"
        );

        delivery = new Delivery();

        delivery.setId(DELIVERY_ID);
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
    void createDeliveryShouldCreateDeliveryWithCustomerId() {
        delivery.setCustomerId(null);

        when(deliveryMapper.toEntity(deliveryRequest))
                .thenReturn(delivery);

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.createDelivery(
                        deliveryRequest,
                        CUSTOMER_ID
                );

        assertSame(deliveryResponse, result);
        assertEquals(CUSTOMER_ID, delivery.getCustomerId());

        verify(deliveryMapper)
                .toEntity(deliveryRequest);

        verify(deliveryRepository)
                .save(delivery);

        verify(deliveryEventProducer)
                .sendDeliveryCreated(
                        DELIVERY_ID,
                        CUSTOMER_ID
                );

        verify(deliveryMapper)
                .toResponse(delivery);
    }

    @Test
    void getAllDeliveriesShouldReturnAllDeliveriesForAdmin() {
        when(deliveryRepository.findAll())
                .thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        null,
                        null,
                        999L,
                        "ADMIN"
                );

        assertEquals(1, result.size());
        assertSame(deliveryResponse, result.getFirst());

        verify(deliveryRepository)
                .findAll();
    }

    @Test
    void getAllDeliveriesShouldFilterByStatusForAdmin() {
        when(deliveryRepository.findByStatus(
                DeliveryStatus.CREATED
        )).thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        DeliveryStatus.CREATED,
                        null,
                        999L,
                        "ADMIN"
                );

        assertEquals(1, result.size());
        assertSame(deliveryResponse, result.getFirst());

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
                .thenReturn(deliveryResponse);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        null,
                        COURIER_ID,
                        999L,
                        "ADMIN"
                );

        assertEquals(1, result.size());
        assertSame(deliveryResponse, result.getFirst());

        verify(deliveryRepository)
                .findByCourierId(COURIER_ID);
    }

    @Test
    void getAllDeliveriesShouldFilterByStatusAndCourierIdForAdmin() {
        delivery.setStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        delivery.setCourierId(COURIER_ID);

        when(deliveryRepository.findByStatusAndCourierId(
                DeliveryStatus.COURIER_ASSIGNED,
                COURIER_ID
        )).thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        DeliveryStatus.COURIER_ASSIGNED,
                        COURIER_ID,
                        999L,
                        "ADMIN"
                );

        assertEquals(1, result.size());
        assertSame(deliveryResponse, result.getFirst());

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
                .thenReturn(deliveryResponse);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        null,
                        null,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertEquals(1, result.size());
        assertSame(deliveryResponse, result.getFirst());

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
                .thenReturn(deliveryResponse);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        DeliveryStatus.CREATED,
                        null,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertEquals(1, result.size());
        assertSame(deliveryResponse, result.getFirst());

        verify(deliveryRepository)
                .findByCustomerIdAndStatus(
                        CUSTOMER_ID,
                        DeliveryStatus.CREATED
                );

        verify(deliveryRepository, never())
                .findAll();
    }

    @Test
    void getAllDeliveriesShouldReturnOnlyCourierDeliveries() {
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findByCourierUserId(
                COURIER_USER_ID
        )).thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        null,
                        null,
                        COURIER_USER_ID,
                        "COURIER"
                );

        assertEquals(1, result.size());
        assertSame(deliveryResponse, result.getFirst());

        verify(deliveryRepository)
                .findByCourierUserId(
                        COURIER_USER_ID
                );

        verify(deliveryRepository, never())
                .findAll();
    }

    @Test
    void getAllDeliveriesShouldReturnOnlyCourierDeliveriesWithStatus() {
        delivery.setCourierUserId(COURIER_USER_ID);
        delivery.setStatus(DeliveryStatus.PICKED_UP);

        when(deliveryRepository.findByCourierUserIdAndStatus(
                COURIER_USER_ID,
                DeliveryStatus.PICKED_UP
        )).thenReturn(List.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        List<DeliveryResponse> result =
                deliveryService.getAllDeliveries(
                        DeliveryStatus.PICKED_UP,
                        null,
                        COURIER_USER_ID,
                        "COURIER"
                );

        assertEquals(1, result.size());
        assertSame(deliveryResponse, result.getFirst());

        verify(deliveryRepository)
                .findByCourierUserIdAndStatus(
                        COURIER_USER_ID,
                        DeliveryStatus.PICKED_UP
                );

        verify(deliveryRepository, never())
                .findAll();
    }

    @Test
    void getAllDeliveriesShouldThrowAccessDeniedForUnknownRole() {
        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.getAllDeliveries(
                        null,
                        null,
                        100L,
                        "UNKNOWN"
                )
        );

        verifyNoInteractions(deliveryMapper);
    }

    @Test
    void getDeliveryByIdShouldReturnDeliveryForOwnerCustomer() {
        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.getDeliveryById(
                        DELIVERY_ID,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertSame(deliveryResponse, result);

        verify(deliveryRepository)
                .findById(DELIVERY_ID);

        verify(deliveryMapper)
                .toResponse(delivery);
    }

    @Test
    void getDeliveryByIdShouldThrowAccessDeniedForAnotherCustomer() {
        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.getDeliveryById(
                        DELIVERY_ID,
                        OTHER_CUSTOMER_ID,
                        "CUSTOMER"
                )
        );

        verify(deliveryMapper, never())
                .toResponse(any());
    }

    @Test
    void getDeliveryByIdShouldReturnAssignedDeliveryForCourier() {
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.getDeliveryById(
                        DELIVERY_ID,
                        COURIER_USER_ID,
                        "COURIER"
                );

        assertSame(deliveryResponse, result);

        verify(deliveryMapper)
                .toResponse(delivery);
    }

    @Test
    void getDeliveryByIdShouldThrowAccessDeniedForAnotherCourier() {
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.getDeliveryById(
                        DELIVERY_ID,
                        OTHER_COURIER_USER_ID,
                        "COURIER"
                )
        );

        verify(deliveryMapper, never())
                .toResponse(any());
    }

    @Test
    void getDeliveryByIdShouldReturnDeliveryForAdmin() {
        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.getDeliveryById(
                        DELIVERY_ID,
                        999L,
                        "ADMIN"
                );

        assertSame(deliveryResponse, result);

        verify(deliveryMapper)
                .toResponse(delivery);
    }

    @Test
    void getDeliveryByIdShouldThrowDeliveryNotFoundException() {
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

        verify(deliveryMapper, never())
                .toResponse(any());
    }

    @Test
    void updateDeliveryShouldUpdateOwnDeliveryForCustomer() {
        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.updateDelivery(
                        DELIVERY_ID,
                        deliveryRequest,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertSame(deliveryResponse, result);

        verify(deliveryMapper)
                .updateEntity(
                        delivery,
                        deliveryRequest
                );

        verify(deliveryRepository)
                .save(delivery);
    }

    @Test
    void updateDeliveryShouldThrowAccessDeniedForAnotherCustomer() {
        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.updateDelivery(
                        DELIVERY_ID,
                        deliveryRequest,
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
    void updateDeliveryShouldThrowAccessDeniedForCourier() {
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.updateDelivery(
                        DELIVERY_ID,
                        deliveryRequest,
                        COURIER_USER_ID,
                        "COURIER"
                )
        );

        verify(deliveryMapper, never())
                .updateEntity(any(), any());

        verify(deliveryRepository, never())
                .save(any());
    }

    @Test
    void updateDeliveryShouldAllowAdmin() {
        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.updateDelivery(
                        DELIVERY_ID,
                        deliveryRequest,
                        999L,
                        "ADMIN"
                );

        assertSame(deliveryResponse, result);

        verify(deliveryMapper)
                .updateEntity(
                        delivery,
                        deliveryRequest
                );

        verify(deliveryRepository)
                .save(delivery);
    }

    @Test
    void assignCourierShouldAssignCourierAndCourierUser() {
        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.assignCourier(
                        DELIVERY_ID,
                        COURIER_ID,
                        COURIER_USER_ID
                );

        assertSame(deliveryResponse, result);

        assertEquals(
                COURIER_ID,
                delivery.getCourierId()
        );

        assertEquals(
                COURIER_USER_ID,
                delivery.getCourierUserId()
        );

        assertEquals(
                DeliveryStatus.COURIER_ASSIGNED,
                delivery.getStatus()
        );

        verify(deliveryRepository)
                .save(delivery);
    }

    @Test
    void assignCourierShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(
                DeliveryStatus.PICKED_UP
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.assignCourier(
                        DELIVERY_ID,
                        COURIER_ID,
                        COURIER_USER_ID
                )
        );

        verify(deliveryRepository, never())
                .save(any());
    }

    @Test
    void pickupDeliveryShouldChangeStatusForAssignedCourier() {
        delivery.setStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        delivery.setCourierId(COURIER_ID);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.pickupDelivery(
                        DELIVERY_ID,
                        COURIER_USER_ID,
                        "COURIER"
                );

        assertSame(deliveryResponse, result);

        assertEquals(
                DeliveryStatus.PICKED_UP,
                delivery.getStatus()
        );

        verify(deliveryRepository)
                .save(delivery);

        verify(deliveryEventProducer)
                .sendDeliveryPickedUp(
                        DELIVERY_ID,
                        CUSTOMER_ID
                );
    }

    @Test
    void pickupDeliveryShouldThrowAccessDeniedForAnotherCourier() {
        delivery.setStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.pickupDelivery(
                        DELIVERY_ID,
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
                .sendDeliveryPickedUp(
                        anyLong(),
                        anyLong()
                );
    }

    @Test
    void pickupDeliveryShouldAllowAdmin() {
        delivery.setStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.pickupDelivery(
                        DELIVERY_ID,
                        999L,
                        "ADMIN"
                );

        assertSame(deliveryResponse, result);

        assertEquals(
                DeliveryStatus.PICKED_UP,
                delivery.getStatus()
        );

        verify(deliveryEventProducer)
                .sendDeliveryPickedUp(
                        DELIVERY_ID,
                        CUSTOMER_ID
                );
    }

    @Test
    void pickupDeliveryShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(
                DeliveryStatus.CREATED
        );

        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.pickupDelivery(
                        DELIVERY_ID,
                        COURIER_USER_ID,
                        "COURIER"
                )
        );

        verify(deliveryRepository, never())
                .save(any());

        verify(deliveryEventProducer, never())
                .sendDeliveryPickedUp(
                        anyLong(),
                        anyLong()
                );
    }

    @Test
    void completeDeliveryShouldChangeStatusForAssignedCourier() {
        delivery.setStatus(
                DeliveryStatus.PICKED_UP
        );

        delivery.setCourierId(COURIER_ID);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.completeDelivery(
                        DELIVERY_ID,
                        COURIER_USER_ID,
                        "COURIER"
                );

        assertSame(deliveryResponse, result);

        assertEquals(
                DeliveryStatus.COMPLETED,
                delivery.getStatus()
        );

        verify(deliveryRepository)
                .save(delivery);

        verify(deliveryEventProducer)
                .sendDeliveryCompleted(
                        DELIVERY_ID,
                        CUSTOMER_ID
                );
    }

    @Test
    void completeDeliveryShouldThrowAccessDeniedForAnotherCourier() {
        delivery.setStatus(
                DeliveryStatus.PICKED_UP
        );

        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.completeDelivery(
                        DELIVERY_ID,
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
                .sendDeliveryCompleted(
                        anyLong(),
                        anyLong()
                );
    }

    @Test
    void completeDeliveryShouldAllowAdmin() {
        delivery.setStatus(
                DeliveryStatus.PICKED_UP
        );

        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.completeDelivery(
                        DELIVERY_ID,
                        999L,
                        "ADMIN"
                );

        assertSame(deliveryResponse, result);

        assertEquals(
                DeliveryStatus.COMPLETED,
                delivery.getStatus()
        );

        verify(deliveryEventProducer)
                .sendDeliveryCompleted(
                        DELIVERY_ID,
                        CUSTOMER_ID
                );
    }

    @Test
    void completeDeliveryShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(
                DeliveryStatus.CREATED
        );

        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.completeDelivery(
                        DELIVERY_ID,
                        COURIER_USER_ID,
                        "COURIER"
                )
        );

        verify(deliveryRepository, never())
                .save(any());

        verify(deliveryEventProducer, never())
                .sendDeliveryCompleted(
                        anyLong(),
                        anyLong()
                );
    }

    @Test
    void cancelDeliveryShouldCancelOwnDeliveryForCustomer() {
        delivery.setStatus(
                DeliveryStatus.CREATED
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.cancelDelivery(
                        DELIVERY_ID,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertSame(deliveryResponse, result);

        assertEquals(
                DeliveryStatus.CANCELLED,
                delivery.getStatus()
        );

        verify(deliveryRepository)
                .save(delivery);

        verify(deliveryEventProducer)
                .sendDeliveryCancelled(
                        DELIVERY_ID,
                        CUSTOMER_ID
                );
    }

    @Test
    void cancelDeliveryShouldCancelCourierAssignedDeliveryForOwnerCustomer() {
        delivery.setStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        delivery.setCourierId(COURIER_ID);
        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.cancelDelivery(
                        DELIVERY_ID,
                        CUSTOMER_ID,
                        "CUSTOMER"
                );

        assertSame(deliveryResponse, result);

        assertEquals(
                DeliveryStatus.CANCELLED,
                delivery.getStatus()
        );

        verify(deliveryRepository)
                .save(delivery);

        verify(deliveryEventProducer)
                .sendDeliveryCancelled(
                        DELIVERY_ID,
                        CUSTOMER_ID
                );
    }

    @Test
    void cancelDeliveryShouldThrowAccessDeniedForAnotherCustomer() {
        delivery.setStatus(
                DeliveryStatus.CREATED
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.cancelDelivery(
                        DELIVERY_ID,
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

        verify(deliveryEventProducer, never())
                .sendDeliveryCancelled(
                        anyLong(),
                        anyLong()
                );
    }

    @Test
    void cancelDeliveryShouldThrowAccessDeniedForCourier() {
        delivery.setStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        delivery.setCourierUserId(COURIER_USER_ID);

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                AccessDeniedException.class,
                () -> deliveryService.cancelDelivery(
                        DELIVERY_ID,
                        COURIER_USER_ID,
                        "COURIER"
                )
        );

        verify(deliveryRepository, never())
                .save(any());

        verify(deliveryEventProducer, never())
                .sendDeliveryCancelled(
                        anyLong(),
                        anyLong()
                );
    }

    @Test
    void cancelDeliveryShouldAllowAdmin() {
        delivery.setStatus(
                DeliveryStatus.CREATED
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        when(deliveryRepository.save(delivery))
                .thenReturn(delivery);

        when(deliveryMapper.toResponse(delivery))
                .thenReturn(deliveryResponse);

        DeliveryResponse result =
                deliveryService.cancelDelivery(
                        DELIVERY_ID,
                        999L,
                        "ADMIN"
                );

        assertSame(deliveryResponse, result);

        assertEquals(
                DeliveryStatus.CANCELLED,
                delivery.getStatus()
        );

        verify(deliveryEventProducer)
                .sendDeliveryCancelled(
                        DELIVERY_ID,
                        CUSTOMER_ID
                );
    }

    @Test
    void cancelDeliveryShouldThrowExceptionForInvalidStatus() {
        delivery.setStatus(
                DeliveryStatus.COMPLETED
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.cancelDelivery(
                        DELIVERY_ID,
                        CUSTOMER_ID,
                        "CUSTOMER"
                )
        );

        verify(deliveryRepository, never())
                .save(any());

        verify(deliveryEventProducer, never())
                .sendDeliveryCancelled(
                        anyLong(),
                        anyLong()
                );
    }

    @Test
    void deleteDeliveryShouldDeleteCancelledDelivery() {
        delivery.setStatus(
                DeliveryStatus.CANCELLED
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        deliveryService.deleteDelivery(
                DELIVERY_ID
        );

        verify(deliveryRepository)
                .delete(delivery);
    }

    @Test
    void deleteDeliveryShouldDeleteCompletedDelivery() {
        delivery.setStatus(
                DeliveryStatus.COMPLETED
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        deliveryService.deleteDelivery(
                DELIVERY_ID
        );

        verify(deliveryRepository)
                .delete(delivery);
    }

    @Test
    void deleteDeliveryShouldThrowExceptionForCreatedDelivery() {
        delivery.setStatus(
                DeliveryStatus.CREATED
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.deleteDelivery(
                        DELIVERY_ID
                )
        );

        verify(deliveryRepository, never())
                .delete(any());
    }

    @Test
    void deleteDeliveryShouldThrowExceptionForCourierAssignedDelivery() {
        delivery.setStatus(
                DeliveryStatus.COURIER_ASSIGNED
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.deleteDelivery(
                        DELIVERY_ID
                )
        );

        verify(deliveryRepository, never())
                .delete(any());
    }

    @Test
    void deleteDeliveryShouldThrowExceptionForPickedUpDelivery() {
        delivery.setStatus(
                DeliveryStatus.PICKED_UP
        );

        when(deliveryRepository.findById(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                InvalidDeliveryStatusException.class,
                () -> deliveryService.deleteDelivery(
                        DELIVERY_ID
                )
        );

        verify(deliveryRepository, never())
                .delete(any());
    }

    @Test
    void deleteDeliveryShouldThrowDeliveryNotFoundException() {
        when(deliveryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                DeliveryNotFoundException.class,
                () -> deliveryService.deleteDelivery(
                        999L
                )
        );

        verify(deliveryRepository, never())
                .delete(any());
    }
}