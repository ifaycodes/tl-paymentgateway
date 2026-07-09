package com.gateway.errors;

public class WrongCardDetail extends RuntimeException {
    public WrongCardDetail(String message) {
        super(message);
    }
}
