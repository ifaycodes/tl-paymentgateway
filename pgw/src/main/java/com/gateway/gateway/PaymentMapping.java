package com.gateway.gateway;

import java.time.LocalDateTime;

import com.gateway.models.PaymentDetail;
import com.gateway.models.PaymentEvent;
import com.gateway.state.State;

public class PaymentMapping {

    public static PaymentEvent createPaymentEvent(String bankTransactionId, String idempotencyKey, State currenState, String paymentRef, String timeCreated, String notes) {
        PaymentEvent paymentEvent = new PaymentEvent();

        paymentEvent.setBankTransactionId(bankTransactionId);
        paymentEvent.setIdempotencyKey(idempotencyKey);
        paymentEvent.setCurrentState(currenState);
        paymentEvent.setPaymentRef(paymentRef);
        paymentEvent.setTimeCreated(timeCreated);
        paymentEvent.setNotes(notes);

        return paymentEvent;
    }

    public static PaymentDetail createPaymentDetail (String paymentRef, String orderId, String customerId, int amount) {
        PaymentDetail paymentDetail = new PaymentDetail();

        paymentDetail.setPaymentRef(paymentRef);
        paymentDetail.setOrderId(orderId);
        paymentDetail.setCustomerId(customerId);
        paymentDetail.setAmount(amount);
        paymentDetail.setCurrentState(State.PENDING);
        paymentDetail.setCreatedAt(LocalDateTime.now().toString());

        return paymentDetail;

    }
}
