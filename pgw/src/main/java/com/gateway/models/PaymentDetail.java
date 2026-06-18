package com.gateway.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gateway.state.State;


@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentDetail {

    private String paymentRef;
    private String orderId;
    private String customerId;
    private int amount;
    private State currentState; //(authorized, captured, voided, refunded)
    private String currency = "USD";
    private String createdAt;


    //getters and setters
    public String getPaymentRef() {
        return paymentRef;
    }
    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }
    
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
    
    public String getCurrency() {
        return currency;
    }

    public State getCurrentState() {
        return currentState;
    }
    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
 
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
