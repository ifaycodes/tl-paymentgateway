package com.gateway.bankconnect;

import java.time.LocalDateTime;

import com.gateway.state.State;

public class BankCaptureResponse {
    private int amount;
    private String authorizationId;
    private String captureId;
    private LocalDateTime capturedAt;
    private String currency;
    private State currentState;

    
    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
    public String getAuthorizationId() {
        return authorizationId;
    }
    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }
    public String getCaptureId() {
        return captureId;
    }
    public void setCaptureId(String captureId) {
        this.captureId = captureId;
    }
    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }
    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public State getCurrentState() {
        return currentState;
    }
    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
}
