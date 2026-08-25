package com.example.courierservice.exception;

public class DuplicateCourierUserException extends RuntimeException {

    public DuplicateCourierUserException(Long userId) {
        super(
                "Пользователь с id "
                        + userId
                        + " уже связан с курьером"
        );
    }
}