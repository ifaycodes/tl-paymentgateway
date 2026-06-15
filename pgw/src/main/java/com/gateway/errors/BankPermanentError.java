package com.gateway.errors;

public class BankPermanentError extends RuntimeException{
    public BankPermanentError(String message) {
        super(message);
    }
}
