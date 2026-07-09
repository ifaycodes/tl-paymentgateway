package com.gateway.errors;

public class WrongTransactionType extends RuntimeException{
    public WrongTransactionType(String message) {
        super(message);
    }
}
