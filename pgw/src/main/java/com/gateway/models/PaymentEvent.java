package com.gateway.models;

import com.gateway.state.State;

public class PaymentEvent {

    private String paymentRef;
    private String idempotencyKey;
    private State currentState;
    private String bankTransactionId;
    private String timeCreated;
    private String notes;


    public String getPaymentRef() {
        return paymentRef;
    }
    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public State getCurrentState() {
        return currentState;
    }
    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }

    public String getBankTransactionId() {
        return bankTransactionId;
    }
    public void setBankTransactionId(String bankTransactionId) {
        this.bankTransactionId = bankTransactionId;
    }
    public String getTimeCreated() {
        return timeCreated;
    }
    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
