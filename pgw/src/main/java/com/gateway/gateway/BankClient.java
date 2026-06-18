package com.gateway.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gateway.bankconnectresponses.BankAuthResponse;
import com.gateway.bankconnectresponses.BankCaptureResponse;
import com.gateway.bankconnectresponses.BankRefundResponse;
import com.gateway.bankconnectresponses.BankVoidResponse;
import com.gateway.data.AuditRepository;
import com.gateway.errors.BankNotConnectingException;
import com.gateway.errors.BankPermanentError;
import com.gateway.errors.ResourceNotFound;
import com.gateway.models.CardDetail;

import javax.sql.DataSource;

@Service
public class BankClient {
        
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String BANK_URL = "http://localhost:8787/api/v1";

    @Autowired
    private AuditRepository auditRepository;

    public BankClient(AuditRepository auditRepository, DataSource dataSource) {
        this.auditRepository = auditRepository;
}


    public BankAuthResponse postAuthorization(CardDetail cardDetail, int amount, String orderId, String paymentRef) throws Exception {
        String id = orderId + amount;
        UUID idempotencyKey = UUID.nameUUIDFromBytes(id.getBytes());

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
                .header("Idempotency-Key", idempotencyKey.toString()) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Replayed: " + response.headers().firstValue("X-Idempotent-Replayed"));
        
        // log response
        auditRepository.logResponse(paymentRef, response.body());
        // check reposnse status code and catch any errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
                ObjectMapper mapper = new ObjectMapper();
                BankAuthResponse bankResponse = mapper.readValue(response.body(), BankAuthResponse.class);
                
                return (bankResponse);
        } else if (statusCode == 500 ) {
                throw new BankNotConnectingException("Bank temporarily unavailable");
        } else {
                // permanent - parse the error body to get the reason
                throw new BankPermanentError("Payment failed: " + response.body());
        }
    }


    public BankAuthResponse getAuthorization(String authorizationId, String paymentRef) throws Exception {

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BANK_URL + "/authorizations/" + authorizationId))
                .GET()
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        //log response
        auditRepository.logResponse(paymentRef, response.body());

        // check reposnse status code and catch any errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
                ObjectMapper mapper = new ObjectMapper();
                BankAuthResponse bankResponse = mapper.readValue(response.body(), BankAuthResponse.class);
                
                return (bankResponse);

        } else if (statusCode == 404 ) {
                throw new ResourceNotFound("Auth-id was not found");
        } else {
                throw new Exception("Something went wrong");
        }
    }


    public BankCaptureResponse postCapture(int amount, String authorizationId, String paymentRef) throws Exception {
        String id = authorizationId + amount;
        UUID idempotencyKey = UUID.nameUUIDFromBytes(id.getBytes());

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
                .header("Idempotency-Key", idempotencyKey.toString()) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        
        // log response
        auditRepository.logResponse(paymentRef, response.body());

        //check response status and catch errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
                ObjectMapper mapper = new ObjectMapper();
                BankCaptureResponse captureResponse = mapper.readValue(response.body(), BankCaptureResponse.class);
                
                return captureResponse;
        } else if (statusCode == 500 ) {
                throw new BankNotConnectingException("Bank temporarily unavailable");
        } else {
                // permanent - parse the error body to get the reason
                throw new BankPermanentError("Payment failed: " + response.body());
        }
        
    }

    public BankCaptureResponse getCapture(String captureId, String paymentRef) throws Exception {

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BANK_URL + "/captures/" + captureId))
                .GET()
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        //log response
        auditRepository.logResponse(paymentRef, response.body());

        // check reposnse status code and catch any errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
                ObjectMapper mapper = new ObjectMapper();
                BankCaptureResponse captureResponse = mapper.readValue(response.body(), BankCaptureResponse.class);
                
                return (captureResponse);

        } else if (statusCode == 404 ) {
                throw new ResourceNotFound("Capture-id was not found");
        } else {
                throw new Exception("Something went wrong");
        }
    }

    public BankVoidResponse postVoid(String authorizationId, String paymentRef) throws Exception {
        String id = authorizationId;
        UUID idempotencyKey = UUID.nameUUIDFromBytes(id.getBytes());

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
                .header("Idempotency-Key", idempotencyKey.toString()) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        

        // log response
        auditRepository.logResponse(paymentRef, response.body());

        //check response status and catch errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
                ObjectMapper mapper = new ObjectMapper();
                BankVoidResponse voidResponse = mapper.readValue(response.body(), BankVoidResponse.class);
                
                return voidResponse;
        } else if (statusCode == 500 ) {
                throw new BankNotConnectingException("Bank temporarily unavailable");
        } else {
                // permanent - parse the error body to get the reason
                throw new BankPermanentError("Payment failed: " + response.body());
        }
        
        
     }

    public BankRefundResponse postRefund(int amount, String captureId, String paymentRef) throws Exception {
        String id = captureId + amount;
        UUID idempotencyKey = UUID.nameUUIDFromBytes(id.getBytes());

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
                .header("Idempotency-Key", idempotencyKey.toString()) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        
        // log response
        auditRepository.logResponse(paymentRef, response.body());

        //check response status and catch errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
                ObjectMapper mapper = new ObjectMapper();
                BankRefundResponse refundResponse = mapper.readValue(response.body(), BankRefundResponse.class);
                
                return refundResponse;
        } else if (statusCode == 500 ) {
                throw new BankNotConnectingException("Bank temporarily unavailable");
        } else {
                // permanent - parse the error body to get the reason
                throw new BankPermanentError("Payment failed: " + response.body());
        }
        
    }

    public BankRefundResponse getRefund(String refundId, String paymentRef) throws Exception {

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BANK_URL + "/refunds/" + refundId))
                .GET()
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        //log response
        auditRepository.logResponse(paymentRef, response.body());

        // check reposnse status code and catch any errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
                ObjectMapper mapper = new ObjectMapper();
                BankRefundResponse refundResponse = mapper.readValue(response.body(), BankRefundResponse.class);
                
                return (refundResponse);

        } else if (statusCode == 404 ) {
                throw new ResourceNotFound("Auth-id was not found");
        } else {
                throw new Exception("Something went wrong");
        }
    }
}