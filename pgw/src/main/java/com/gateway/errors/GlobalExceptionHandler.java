package com.gateway.errors;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BankNotConnectingException.class)
    public ResponseEntity<?> handleTranscientError(BankNotConnectingException e) {
        return ResponseEntity.status(503).body(Map.of(
            "error", "Bank_UNAVAILABLE",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(BankPermanentError.class)
    public ResponseEntity<?> handlePermanentError(BankPermanentError e) throws JsonMappingException, JsonProcessingException {
        return ResponseEntity.status(402).body(Map.of(
            "error", "PAYMENT_FAILED",
            "message", extractMessage(e.getMessage())
        ));
    }

    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<?> handlePermanentError(ResourceNotFound e) throws JsonMappingException, JsonProcessingException {
        return ResponseEntity.status(404).body(Map.of(
            "error", "NOT_FOUND",
            "message", extractMessage(e.getMessage())
        ));
    }

    @ExceptionHandler(Exception.class) // catches anything else
    public ResponseEntity<?> handleGenericErrors(Exception e) throws JsonMappingException, JsonProcessingException {
        return ResponseEntity.status(500).body(Map.of(
            "error", "INTERNAL_ERROR",
            "message", "Something went wrong. " + extractMessage(e.getMessage())
        ));
    }

    @ExceptionHandler(WrongCardDetail.class)
    public ResponseEntity<?> handleWrongCardDetailError(WrongCardDetail e) {
        return ResponseEntity.status(400).body(Map.of(
            "error", "BAD_REQUEST",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(WrongTransactionType.class)
    public ResponseEntity<?> handleWrongTransactionTypeError(WrongTransactionType e) {
        return ResponseEntity.status(400).body(Map.of(
            "error", "BAD_REQUEST",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(InvalidOrderDetail.class)
    public ResponseEntity<?> handleInvalidOrderDetailError(InvalidOrderDetail e) {
        return ResponseEntity.status(400).body(Map.of(
            "error", "BAD_REQUEST",
            "message", e.getMessage()
        ));
    }



    public String extractMessage(String message) throws JsonMappingException, JsonProcessingException {
        String rawError = message;

        int jsonStart = rawError.indexOf("{");
        int jsonEnd = rawError.lastIndexOf("}");

        if (jsonStart != -1 && jsonEnd != -1) {
            String jsonText = rawError.substring(jsonStart, jsonEnd + 1);
            
            // Parse the JSON text and grab the "message" key
            ObjectMapper mapper =new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonText);
            if (rootNode.has("message")) {
                String clientMessage = rootNode.path("message").asText();
                return clientMessage;
            }
        }
        return "Payment failed. Please try again.";
    }
}
