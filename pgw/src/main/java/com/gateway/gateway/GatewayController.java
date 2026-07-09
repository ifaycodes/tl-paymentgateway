package com.gateway.gateway;

import java.sql.SQLException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.bankconnectresponses.BankAuthResponse;
import com.gateway.bankconnectresponses.BankCaptureResponse;
import com.gateway.bankconnectresponses.BankRefundResponse;
import com.gateway.data.PaymentRepository;
import com.gateway.models.OrderDetail;
import com.gateway.models.PaymentDetail;
import com.gateway.proof.Receipt;
import com.gateway.state.State;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payments", description = "API endpoints for making payments")
public class GatewayController {

    private final Receipt receipt;
    private final BankClient bankClient;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    //constructor
    public GatewayController(PaymentService paymentService, PaymentRepository paymentRepository, BankClient bankClient, Receipt receipt) {
        this.paymentRepository = paymentRepository;
        this.bankClient = bankClient;
        this.receipt = receipt;
        this.paymentService = paymentService;
    }

    // api mappings

    @PostMapping("/auth")
    @Operation(summary = "Authorize a payment to reserve fund")
    public ResponseEntity<String> authorize(@RequestBody OrderDetail orderDetail) throws Exception {
        String bankAuthResponse = paymentService.authorizePayment(orderDetail);
        return ResponseEntity.ok().body(bankAuthResponse);
    }

    //get auth with id
    @GetMapping("/getauth")
    @Operation(summary = "Return bank auth response detail for this particular ref")
    public BankAuthResponse getAuthorization(@RequestParam String paymentRef) throws Exception {
        String authorizationId = paymentService.getBankTransactionId(paymentRef, State.APPROVED);
        BankAuthResponse bankAuthResponse = bankClient.getAuthorization(authorizationId, paymentRef);
        return bankAuthResponse;
    }

    //send capture request
    @PostMapping("/capt")
    @Operation(summary = "Capture a payment - Deduct amt from customer card")
    public ResponseEntity<String> capture(@RequestBody String paymentRef) throws Exception {
        String response = paymentService.capturePayment(paymentRef);
        return ResponseEntity.ok().body(response);
    }

    // return bank capture response
    @GetMapping("/getcapt")
    @Operation(summary = "Return bank capture response detail for this particular ref")
    public BankCaptureResponse getCapture(@RequestParam String paymentRef) throws Exception {
        String captureId = paymentService.getBankTransactionId(paymentRef, State.CAPTURED);
        BankCaptureResponse captureResponse = bankClient.getCapture(captureId, paymentRef);
        return captureResponse;
    }

    @PostMapping("/void")
    @Operation(summary = "Void an authorized transaction - release reserved fund")
    public ResponseEntity<String> voids(@RequestBody String paymentRef) throws Exception {
        String response = paymentService.voidPayment(paymentRef);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/ref")
    @Operation(summary = "Refund a deducted amount back to customer")
    public ResponseEntity<String> refund(@RequestBody String paymentRef) throws Exception {
        String response = paymentService.refund(paymentRef);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/getref")
    @Operation(summary = "Return bank refund response detail for this particular ref")
    public BankRefundResponse getRefund(@RequestParam String paymentRef) throws Exception {
        String refundId = paymentService.getBankTransactionId(paymentRef, State.REFUNDED);
        BankRefundResponse refundResponse = bankClient.getRefund(refundId, paymentRef);
        return refundResponse;
    }

    @PostMapping("/receipts/{orderId}")
    @Operation(summary = "Create receipt for an order")
    public String receipts(@PathVariable String orderId) throws SQLException {
        return receipt.createReceipt(orderId);
    }

    @GetMapping("/orders")
    @Operation(summary = "Get orders from customer")
    public PaymentDetail getOrdersByCustomerId(@RequestParam String customerId) throws SQLException {
        return paymentRepository.findByCustomerId(customerId);
    }

    @GetMapping("/status")
    @Operation(summary = "find the order with this id and see status")
    public PaymentDetail getOrderStatus(@RequestParam String orderId) throws SQLException {
        return paymentRepository.findByOrderId(orderId);
    }

}
