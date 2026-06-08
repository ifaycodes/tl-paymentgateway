package com.gateway.bankconnect;

import java.time.LocalDateTime;

import com.gateway.state.State;

public class BankAuthResponse {

    private int amount;
    private String authorizationId;
    private LocalDateTime createdAt;
    private String currency;
    private LocalDateTime expiresAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public State getCurrentState() {
        return currentState;
    }
    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
}
