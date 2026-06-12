package com.gateway.bankconnect;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import com.gateway.carddetails.CardDetail;

@Service
public class BankClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String BANK_URL = "http://localhost:8787/api/v1";

    public BankAuthResponse authorization(CardDetail cardDetail, int amount) throws Exception {

        // http request body
        String requestBody = """
                {
                    "amount": %d,
                    "card_number": "%s",
                    "cvv": "%s",
                    "expiry_month": %d,
                    "expiry_year": %d
                }
                """.formatted(amount, cardDetail.getCardNumber(), 
                              cardDetail.getCvv(), cardDetail.getExpiryMonth(), cardDetail.getExpiryYear());

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BANK_URL + "/authorizations"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString()) // the idempptency key parameter
                .build();

        System.out.println("Sending to bank: " + requestBody);

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        
        System.out.println("Bank response: " + response.body());

        // Parse and return the bank's response
        ObjectMapper mapper = new ObjectMapper();
        BankAuthResponse bankResponse = mapper.readValue(response.body(), BankAuthResponse.class);
        System.out.println(bankResponse);
        return bankResponse;

    }

    public BankCaptureResponse capture(int amount, String authorizationId) throws Exception {

        // http request body
        String requestBody = """
                {
                    "amount": %d,
                    "authorization_id": "%s"
                }
                """.formatted(amount, authorizationId);

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BANK_URL + "/captures"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString()) // the idempptency key parameter
                .build();

        System.out.println("Sending to bank: " + requestBody);

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Bank response: " + response.body());

        // Parse and return the bank's response
        ObjectMapper mapper = new ObjectMapper();
        BankCaptureResponse captureResponse = mapper.readValue(response.body(), BankCaptureResponse.class);
        System.out.println(captureResponse);
        return captureResponse;
    }

    public BankVoidResponse voidOrder(String authorizationId) throws Exception {

        // http request body
        String requestBody = """
                {
                    "authorization_id": "%s"
                }
                """.formatted(authorizationId);

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BANK_URL + "/voids"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString()) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        System.out.println(response);

        // Parse and return the bank's response
        ObjectMapper mapper = new ObjectMapper();
        BankVoidResponse voidResponse = mapper.readValue(response.body(), BankVoidResponse.class);
        System.out.println(voidResponse);
        return voidResponse;
        
     }

    public BankRefundResponse refund(int amount, String captureId) throws Exception {

        // http request body
        String requestBody = """
                {
                    "amount": %d,
                    "capture_id": "%s"
                }
                """.formatted(amount, captureId);

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BANK_URL + "/refunds"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString()) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        // Parse and return the bank's response
        ObjectMapper mapper = new ObjectMapper();
        BankRefundResponse refundResponse = mapper.readValue(response.body(), BankRefundResponse.class);
        System.out.println(refundResponse);
        return refundResponse;
    }
}