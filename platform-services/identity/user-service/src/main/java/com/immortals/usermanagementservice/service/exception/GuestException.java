package com.immortals.usermanagementservice.service.exception;

public class GuestException extends RuntimeException {
    public GuestException(String s, Exception e) {
        super(s,e);
    }
}
