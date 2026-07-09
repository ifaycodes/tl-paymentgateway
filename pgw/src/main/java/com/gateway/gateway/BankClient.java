package com.gateway.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.gateway.bankconnectresponses.BankAuthResponse;
import com.gateway.bankconnectresponses.BankCaptureResponse;
import com.gateway.bankconnectresponses.BankRefundResponse;
import com.gateway.bankconnectresponses.BankVoidResponse;
import com.gateway.data.AuditRepository;
import com.gateway.errors.BankNotConnectingException;
import com.gateway.errors.BankPermanentError;
import com.gateway.errors.ResourceNotFound;
import com.gateway.models.OrderDetail;

@Service
public class BankClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private final AuditRepository auditRepository;
    private final String bankUrl;

    public BankClient(AuditRepository auditRepository, @Value("${bank.api.base-url:http://localhost:8787/api/v1}") String bankUrl) {
        this.auditRepository = auditRepository;
        this.bankUrl = bankUrl;
    }


    public BankAuthResponse postAuthorization(OrderDetail orderDetail, int amount, String orderId, String paymentRef, String idempotencyKey) throws Exception {

        // http request body
        String requestBody = """
                {
                    "amount": %d,
                    "card_number": "%s",
                    "cvv": "%s",
                    "expiry_month": %d,
                    "expiry_year": %d
                }
                """.formatted(amount, orderDetail.getCardNumber(),
                              orderDetail.getCvv(), orderDetail.getExpiryMonth(), orderDetail.getExpiryYear());

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(bankUrl + "/authorizations"))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        // log response
        auditRepository.logResponse(paymentRef, response.body());
        // check reposnse status code and catch any errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
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
                .uri(URI.create(bankUrl + "/authorizations/" + authorizationId))
                .timeout(REQUEST_TIMEOUT)
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
                BankAuthResponse bankResponse = mapper.readValue(response.body(), BankAuthResponse.class);

                return (bankResponse);

        } else if (statusCode == 404 ) {
                throw new ResourceNotFound("Auth-id was not found");
        } else {
                throw new Exception("Something went wrong");
        }
    }


    public BankCaptureResponse postCapture(int amount, String authorizationId, String paymentRef, String idempotencyKey) throws Exception {

        // http request body
        String requestBody = """
                {
                    "amount": %d,
                    "authorization_id": "%s"
                }
                """.formatted(amount, authorizationId);

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(bankUrl + "/captures"))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());


        // log response
        auditRepository.logResponse(paymentRef, response.body());

        //check response status and catch errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
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
                .uri(URI.create(bankUrl + "/captures/" + captureId))
                .timeout(REQUEST_TIMEOUT)
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
                BankCaptureResponse captureResponse = mapper.readValue(response.body(), BankCaptureResponse.class);

                return (captureResponse);

        } else if (statusCode == 404 ) {
                throw new ResourceNotFound("Capture-id was not found");
        } else {
                throw new Exception("Something went wrong");
        }
    }

    public BankVoidResponse postVoid(String authorizationId, String paymentRef, String idempotencyKey) throws Exception {

        // http request body
        String requestBody = """
                {
                    "authorization_id": "%s"
                }
                """.formatted(authorizationId);

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(bankUrl + "/voids"))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());


        // log response
        auditRepository.logResponse(paymentRef, response.body());

        //check response status and catch errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
                BankVoidResponse voidResponse = mapper.readValue(response.body(), BankVoidResponse.class);

                return voidResponse;
        } else if (statusCode == 500 ) {
                throw new BankNotConnectingException("Bank temporarily unavailable");
        } else {
                // permanent - parse the error body to get the reason
                throw new BankPermanentError("Payment failed: " + response.body());
        }


     }

    public BankRefundResponse postRefund(int amount, String captureId, String paymentRef, String idempotencyKey) throws Exception {

        // http request body
        String requestBody = """
                {
                    "amount": %d,
                    "capture_id": "%s"
                }
                """.formatted(amount, captureId);

        // building the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(bankUrl + "/refunds"))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey) // the idempptency key parameter
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());


        // log response
        auditRepository.logResponse(paymentRef, response.body());

        //check response status and catch errors
        int statusCode = response.statusCode();

        if (statusCode == 200) {
                // Success, Parse and return the bank's response
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
                .uri(URI.create(bankUrl + "/refunds/" + refundId))
                .timeout(REQUEST_TIMEOUT)
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
                BankRefundResponse refundResponse = mapper.readValue(response.body(), BankRefundResponse.class);

                return (refundResponse);

        } else if (statusCode == 404 ) {
                throw new ResourceNotFound("Auth-id was not found");
        } else {
                throw new Exception("Something went wrong");
        }
    }
}
