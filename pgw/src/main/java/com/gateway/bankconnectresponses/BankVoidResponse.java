package com.gateway.bankconnectresponses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BankVoidResponse {


    @JsonProperty("authorization_id")
    private String authorizationId;

    @JsonProperty("status")
    private String currentState;

    @JsonProperty("void_id")
    private String voidId;

    @JsonProperty("voided_at")
    private String voidedAt;

    
    public String getAuthorizationId() {
        return authorizationId;
    }
    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }
    public String getCurrentState() {
        return currentState;
    }
    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }
    public String getVoidId() {
        return voidId;
    }
    public void setVoidId(String voidId) {
        this.voidId = voidId;
    }
    public String getVoidedAt() {
        return voidedAt;
    }
    public void setVoidedAt(String voidedAt) {
        this.voidedAt = voidedAt;
    }

}
