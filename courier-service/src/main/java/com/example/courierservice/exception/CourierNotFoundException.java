package com.example.courierservice.exception;

public class CourierNotFoundException extends RuntimeException {

    public CourierNotFoundException(Long id) {
        super("Курьер с id " + id + " не найден");
    }
}