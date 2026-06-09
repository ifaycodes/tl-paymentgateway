package com.gateway.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gateway.state.State;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDetail {

    private String orderId;
    private String customerId;
    private int amount;
    private String paymentRef;
    private static String currency = "USD";
    private State currentState; //(authorized, captured, voided, refunded)
    private String authorizationId;
    private String captureId;
    private String voidId;
    private String refundId;
    private String createdAt;
    private String capturedAt;
    private String voidedAt;
    private String refundedAt;


    //getters and setters
    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
    public String getPaymentRef() {
        return paymentRef;
    }
    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }
    public static String getCurrency() {
        return currency;
    }
    public State getCurrentState() {
        return currentState;
    }
    public void setCurrentState(State currentState) {
        this.currentState = currentState;
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
    public String getVoidId() {
        return voidId;
    }
    public void setVoidId(String voidId) {
        this.voidId = voidId;
    }
    public String getRefundId() {
        return refundId;
    }
    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    public String getCapturedAt() {
        return capturedAt;
    }
    public void setCapturedAt(String capturedAt) {
        this.capturedAt = capturedAt;
    }
    public String getVoidedAt() {
        return voidedAt;
    }
    public void setVoidedAt(String voidedAt) {
        this.voidedAt = voidedAt;
    }
    public String getRefundedAt() {
        return refundedAt;
    }
    public void setRefundedAt(String refundedAt) {
        this.refundedAt = refundedAt;
    }

}
