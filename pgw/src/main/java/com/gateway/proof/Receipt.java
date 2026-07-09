package com.gateway.proof;

import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gateway.data.PaymentEventRepository;
import com.gateway.data.PaymentRepository;
import com.gateway.models.PaymentDetail;
import com.gateway.models.PaymentEvent;
import com.gateway.state.State;

@Service
public class Receipt {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    //constructor
    public Receipt(PaymentRepository paymentRepository, PaymentEventRepository paymentEventRepository) {
            this.paymentRepository = paymentRepository;
            this.paymentEventRepository = paymentEventRepository;
        }

    //creating a receipt
    public String createReceipt(String orderId) throws SQLException {
        PaymentDetail order = paymentRepository.findByOrderId(orderId);
        State currentState = order.getCurrentState();
        String customerId = order.getCustomerId();
        double amount = order.getAmount() / 100.0;
        String paymentRef = order.getPaymentRef();

        List<PaymentEvent> paymentEvent = paymentEventRepository.findByPaymentRef(paymentRef);

        StringBuilder receipt = new StringBuilder();

        receipt.append("This receipt is for order " + orderId + "\n");
        receipt.append("Customer ID: " + customerId + "\n Order amount: " + amount + "usd\n Payment reference: " + paymentRef + "\n");
        receipt.append("Current State of order: " + currentState + "\n");

        for (PaymentEvent event : paymentEvent) {
            switch (event.getCurrentState()) {
                case APPROVED -> receipt.append("Authorization ID: " + event.getBankTransactionId() + ", authorized at: " + truncateTimestamp(event.getTimeCreated(), 20) + "\n");
                case CAPTURED -> receipt.append("Capture ID: " + event.getBankTransactionId() + ", captured at: " + truncateTimestamp(event.getTimeCreated(), 20) + "\n");
                case VOIDED ->  receipt.append("Voided ID: " + event.getBankTransactionId() + ", captured at: " + truncateTimestamp(event.getTimeCreated(), 19) + "\n");
                case REFUNDED -> receipt.append("Refund ID: " + event.getBankTransactionId() + ", refunded at: " + truncateTimestamp(event.getTimeCreated(), 19) + "\n");
                case FAILED -> receipt.append("This payment failed. Reason: " + event.getNotes());
                default -> receipt.append("This payment is still pending");
            }
        }

        return receipt.toString();
    }

    
    private String truncateTimestamp(String timestamp, int maxLen) {
        if (timestamp == null) {
            return "";
        }
        return timestamp.length() > maxLen ? timestamp.substring(0, maxLen) : timestamp;
    }
}
