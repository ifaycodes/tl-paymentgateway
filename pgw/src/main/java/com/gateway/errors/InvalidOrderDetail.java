package com.gateway.errors;

public class InvalidOrderDetail extends RuntimeException {
    public InvalidOrderDetail(String message) {
        super(message);
    }
}
