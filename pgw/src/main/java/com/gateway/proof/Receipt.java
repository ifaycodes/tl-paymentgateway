package com.gateway.proof;

import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import com.gateway.data.PaymentRepository;
import com.gateway.models.OrderDetail;
import com.gateway.state.State;

public class Receipt {
    public String orderId;
    public String customerId;
    public int amount;
    public String currency = OrderDetail.getCurrency();
    private State currentState; //(authorized, captured, voided, refunded)
    public String paymentRef;
    public int bankRefId;
    public LocalDateTime timeStamp;

    private final DataSource dataSource = null;
    PaymentRepository paymentRepository = new PaymentRepository(dataSource);
    

    //constructor
    public Receipt() //(String orderId, String customerId, String currentState, String amount) 
        {}

    //creating a receipt
    public String createReceipt(String orderId, String customerId, String amount) throws SQLException {
        currentState = paymentRepository.findByOrderId(orderId);
        paymentRef = paymentRepository.findByCustomerId(customerId).getPaymentRef();
        OrderDetail order = paymentRepository.findByPaymentRef(paymentRef);

        StringBuilder receipt = new StringBuilder();

        receipt.append("This receipt is for order " + orderId + "\n");
        receipt.append("Customer ID: " + customerId + "\n Order amount: " + amount + "usd\n Payment reference: " + paymentRef + "\n");
        receipt.append("Current State of order: " + currentState);
        receipt.append("Authorization ID: " + order.getAuthorizationId() + ", authorized at: " + order.getCreatedAt() + "\n");
        
        if (currentState == State.CAPTURED) {
            receipt.append("Capture ID: " + order.getCaptureId() + ", captured at: " + order.getCapturedAt() + "\n");
        } else if (currentState == State.REFUNDED) {
            receipt.append("Captured ID: " + order.getCaptureId() + ", captured at: " + order.getCapturedAt() + "\n");
            receipt.append("Refund ID: " + order.getRefundId() + ", refunded at: " + order.getRefundedAt() + "\n");
        } else if (currentState == State.VOIDED) {
            receipt.append("Voided ID: " + order.getVoidId() + ", captured at: " + order.getVoidedAt() + "\n");
        }



        return receipt.toString();
    }

    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
}
