package com.example.deliveryservice.exception;

public class InvalidDeliveryStatusException extends RuntimeException {

    public InvalidDeliveryStatusException(String message) {
        super(message);
    }
}