package com.gateway.errors;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BankNotConnectingException.class)
    public ResponseEntity<?> handleTranscientError(BankNotConnectingException e) {
        return ResponseEntity.status(503).body(Map.of(
            "error", "Bank_UNAVAILABLE",
            "message", "Please retry"
        ));
    }

    @ExceptionHandler(BankPermanentError.class)
    public ResponseEntity<?> handlePermanentError(BankPermanentError e) {
        return ResponseEntity.status(402).body(Map.of(
            "error", "PAYMENT_FAILED",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<?> handlePermanentError(ResourceNotFound e) {
        return ResponseEntity.status(404).body(Map.of(
            "error", "NOT_FOUND",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class) // catches anything else
    public ResponseEntity<?> handleGenericErrors(Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "error", "INTERNAL_ERROR",
            "message", "Something went wrong"
        ));
    }
}
