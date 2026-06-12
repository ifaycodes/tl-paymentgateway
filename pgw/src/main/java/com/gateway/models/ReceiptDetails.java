package com.gateway.models;

import java.time.LocalDateTime;

public class ReceiptDetails {
    private int receiptId;
    private String paymentRef;
    private LocalDateTime dateCreated;

    
    public int getReceiptId() {
        return receiptId;
    }
    public void setReceiptId(int receiptId) {
        this.receiptId = receiptId;
    }
    public String getPaymentRef() {
        return paymentRef;
    }
    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

}
