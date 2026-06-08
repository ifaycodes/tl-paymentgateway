package com.gateway.models;

import java.time.LocalDateTime;

import com.gateway.state.State;

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
    private LocalDateTime createdAt;
    private LocalDateTime capturedAt;
    private LocalDateTime voidedAt;
    private LocalDateTime refundedAt;


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
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }
    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }
    public LocalDateTime getVoidedAt() {
        return voidedAt;
    }
    public void setVoidedAt(LocalDateTime voidedAt) {
        this.voidedAt = voidedAt;
    }
    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }
    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

}
