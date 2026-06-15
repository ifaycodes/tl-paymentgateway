package com.gateway.proof;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gateway.data.PaymentEventRepository;
import com.gateway.data.PaymentRepository;
import com.gateway.models.PaymentDetail;
import com.gateway.models.PaymentEvent;
import com.gateway.state.State;

import javax.sql.DataSource;

@Service
public class Receipt {
    public String orderId;
    public String customerId;
    public int amount;
    public String currency = "USD";
    private State currentState; //(authorized, captured, voided, refunded)
    public String paymentRef;
    public int bankRefId;
    public LocalDateTime timeStamp;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;
    

    //constructor
    public Receipt(DataSource dataSource, PaymentRepository paymentRepository, PaymentEventRepository paymentEventRepository) {
            this.paymentRepository = paymentRepository;
            this.paymentEventRepository = paymentEventRepository;
        }

    //creating a receipt
    public String createReceipt(String orderId) throws SQLException {
        PaymentDetail order = paymentRepository.findByOrderId(orderId);
        currentState = order.getCurrentState();
        customerId = order.getCustomerId();
        amount = order.getAmount();
        paymentRef = order.getPaymentRef();

        List<PaymentEvent> paymentEvent = paymentEventRepository.findByPaymentRef(paymentRef);

        StringBuilder receipt = new StringBuilder();

        receipt.append("This receipt is for order " + orderId + "\n");
        receipt.append("Customer ID: " + customerId + "\n Order amount: " + amount + "usd\n Payment reference: " + paymentRef + "\n");
        receipt.append("Current State of order: " + currentState + "\n");

        for (PaymentEvent event : paymentEvent) {
            switch (event.getCurrentState()) {
                case APPROVED -> receipt.append("Authorization ID: " + event.getBankTransactionId() + ", authorized at: " + event.getTimeCreated().substring(0, 20) + "\n");
                case CAPTURED -> receipt.append("Capture ID: " + event.getBankTransactionId() + ", captured at: " + event.getTimeCreated().substring(0, 20) + "\n");
                case VOIDED ->  receipt.append("Voided ID: " + event.getBankTransactionId() + ", captured at: " + event.getTimeCreated().substring(0, 19) + "\n");
                case REFUNDED -> receipt.append("Refund ID: " + event.getBankTransactionId() + ", refunded at: " + event.getTimeCreated().substring(0, 19) + "\n");
                case FAILED -> receipt.append("This payment failed. Reason: " + event.getNotes());
                default -> receipt.append("This payment is still pending");
            }
        }

        return receipt.toString();
    }

    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
}
