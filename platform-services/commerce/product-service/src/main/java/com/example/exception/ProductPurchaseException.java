package com.example.exception;

public class ProductPurchaseException extends RuntimeException {
    public ProductPurchaseException(String s) {
        super(s);
    }
}
