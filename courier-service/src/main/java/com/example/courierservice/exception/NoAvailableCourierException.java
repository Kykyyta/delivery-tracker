package com.example.courierservice.exception;

public class NoAvailableCourierException extends RuntimeException {

    public NoAvailableCourierException() {
        super("Нет доступных курьеров");
    }
}