package com.gateway.bankconnect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BankVoidResponse {

    private String authorizationId;
    private String currentState;
    private String voidId;
    private String voidedAt;

    
    public String getAuthorizationId() {
        return authorizationId;
    }
    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }
    public String getCurrentState() {
        return currentState;
    }
    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }
    public String getVoidId() {
        return voidId;
    }
    public void setVoidId(String voidId) {
        this.voidId = voidId;
    }
    public String getVoidedAt() {
        return voidedAt;
    }
    public void setVoidedAt(String voidedAt) {
        this.voidedAt = voidedAt;
    }

}
