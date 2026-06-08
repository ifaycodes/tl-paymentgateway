package com.gateway.bankconnect;

import java.time.LocalDateTime;

import com.gateway.state.State;

public class BankRefundResponse {

    private int amount;
    private String captureId;
    private String currency;
    private String refundId;
    private LocalDateTime refundedAt;
    private State currentState;

    
    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
    public String getCaptureId() {
        return captureId;
    }
    public void setCaptureId(String captureId) {
        this.captureId = captureId;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public String getRefundId() {
        return refundId;
    }
    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }
    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }
    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
    public State getCurrentState() {
        return currentState;
    }
    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
}
