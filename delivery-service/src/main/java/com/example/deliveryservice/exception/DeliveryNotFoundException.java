package com.example.deliveryservice.exception;

public class DeliveryNotFoundException extends RuntimeException {

    public DeliveryNotFoundException(Long id) {
        super("Доставка с id " + id + " не найдена");
    }
}
