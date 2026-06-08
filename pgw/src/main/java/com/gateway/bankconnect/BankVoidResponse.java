package com.gateway.bankconnect;

import java.time.LocalDateTime;

import com.gateway.state.State;

public class BankVoidResponse {

    private String authorizationId;
    private State currentState;
    private String voidId;
    private LocalDateTime voidedAt;

    
    public String getAuthorizationId() {
        return authorizationId;
    }
    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }
    public State getCurrentState() {
        return currentState;
    }
    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
    public String getVoidId() {
        return voidId;
    }
    public void setVoidId(String voidId) {
        this.voidId = voidId;
    }
    public LocalDateTime getVoidedAt() {
        return voidedAt;
    }
    public void setVoidedAt(LocalDateTime voidedAt) {
        this.voidedAt = voidedAt;
    }

}
