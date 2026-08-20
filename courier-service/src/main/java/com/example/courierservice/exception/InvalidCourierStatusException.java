package com.example.courierservice.exception;

public class InvalidCourierStatusException extends RuntimeException {

    public InvalidCourierStatusException(String message) {
        super(message);
    }
}