package com.example.courierservice.exception;

public class DuplicateCourierPhoneException extends RuntimeException {

    public DuplicateCourierPhoneException(String phone) {
        super("Курьер с телефоном " + phone + " уже существует");
    }
}