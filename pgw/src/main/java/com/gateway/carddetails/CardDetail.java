package com.gateway.carddetails;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CardDetail {
    @JsonProperty("card_number")
    private String cardNumber;
    
    @JsonProperty("expiry_month")
    private int expiryMonth;
    
    @JsonProperty("expiry_year")
    private int expiryYear;
    
    @JsonProperty("cvv")
    private String cvv;


    public CardDetail() {
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
