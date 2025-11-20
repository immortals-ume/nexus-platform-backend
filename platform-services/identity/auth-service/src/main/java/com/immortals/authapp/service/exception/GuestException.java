package com.immortals.authapp.service.exception;

public class GuestException extends RuntimeException {
    public GuestException(String s, Exception e) {
        super(s,e);
    }
}
