package com.gateway.bankconnectresponses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BankRefundResponse {

    private int amount;
    private String idempotencyKey;

    @JsonProperty("capture_id")
    private String captureId;

    private String currency;

    @JsonProperty("refund_id")
    private String refundId;

    @JsonProperty("refunded_at")
    private String refundedAt;

    @JsonProperty("status")
    private String currentState;

    
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
    public String getRefundedAt() {
        return refundedAt;
    }
    public void setRefundedAt(String refundedAt) {
        this.refundedAt = refundedAt;
    }
    public String getCurrentState() {
        return currentState;
    }
    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
