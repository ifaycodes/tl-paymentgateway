package com.gateway.errors;

public class BankNotConnectingException extends RuntimeException{
    public BankNotConnectingException(String message) {
        super(message);
    }
}
