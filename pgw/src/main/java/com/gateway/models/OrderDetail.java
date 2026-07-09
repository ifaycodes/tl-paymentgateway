package com.gateway.models;

public class OrderDetail {
    private int amt;
    private String orderId;
    private String customerId;
    private String cardNumber;
    private String cvv;
    private int expiryMonth;
    private int expiryYear;
    
    public int getAmt() {
        return amt;
    }
    public void setAmt(int amt) {
        this.amt = amt;
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
    public String getCardNumber() {
        return cardNumber;
    }
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    public String getCvv() {
        return cvv;
    }
    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
    public int getExpiryMonth() {
        return expiryMonth;
    }
    public void setExpiryMonth(int expiryMonth) {
        this.expiryMonth = expiryMonth;
    }
    public int getExpiryYear() {
        return expiryYear;
    }
    public void setExpiryYear(int expiryYear) {
        this.expiryYear = expiryYear;
    }


}