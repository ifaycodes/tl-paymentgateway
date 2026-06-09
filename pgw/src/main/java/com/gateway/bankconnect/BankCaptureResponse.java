package com.gateway.bankconnect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BankCaptureResponse {
    private int amount;

    @JsonProperty("auntorization_id")
    private String authorizationId;

    @JsonProperty("capture_id")
    private String captureId;

    @JsonProperty("captured_at")
    private String capturedAt;

    private String currency;

    @JsonProperty("status")
    private String 
    currentState;

    
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
    public String getCapturedAt() {
        return capturedAt;
    }
    public void setCapturedAt(String capturedAt) {
        this.capturedAt = capturedAt;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public String 
    getCurrentState() {
        return currentState;
    }
    public void setCurrentState(String 
        currentState) {
        this.currentState = currentState;
    }
}
