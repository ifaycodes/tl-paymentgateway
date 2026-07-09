package com.gateway.testing;

import com.gateway.bankconnectresponses.BankAuthResponse;
import com.gateway.bankconnectresponses.BankCaptureResponse;
import com.gateway.bankconnectresponses.BankRefundResponse;
import com.gateway.data.AuditRepository;
import com.gateway.data.PaymentEventRepository;
import com.gateway.data.PaymentRepository;
import com.gateway.errors.InvalidOrderDetail;
import com.gateway.errors.ResourceNotFound;
import com.gateway.errors.WrongCardDetail;
import com.gateway.errors.WrongTransactionType;
import com.gateway.gateway.*;
import com.gateway.models.OrderDetail;
import com.gateway.models.PaymentDetail;
import com.gateway.models.PaymentEvent;
import com.gateway.pgw.PgwApplication;
import com.gateway.proof.Receipt;
import com.gateway.state.State;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

@SpringBootTest(classes = PgwApplication.class)
@AutoConfigureTestDatabase
@ExtendWith(MockitoExtension.class)
public class BankTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private Receipt receipt;

    @Autowired
    private BankClient bankClient;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private GatewayController gatewayApis;

    @BeforeEach
    void setUp() {
        bankClient = new BankClient(auditRepository, "http://localhost:8787/api/v1");
        gatewayApis = new GatewayController(paymentService, paymentRepository
            , bankClient, receipt);

    }

    String cardNumber = "4111111111111111";
    String cvv = "123";
    int expiryMonth = 12;
    int expiryYear = 30;
    int amount = 1000;
    String customerId = "cus001";
    String orderId ="ord001";
    String paymentRef = amount + orderId;
    String idemKey = "test-idem-key-123";

    private OrderDetail buildOrderDetail() {
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setAmt(amount);
        orderDetail.setOrderId(orderId);
        orderDetail.setCustomerId(customerId);
        orderDetail.setCardNumber(cardNumber);
        orderDetail.setCvv(cvv);
        orderDetail.setExpiryMonth(expiryMonth);
        orderDetail.setExpiryYear(expiryYear);
        return orderDetail;
    }

    // this only proves the mock bank itself dedupes by Idempotency-Key header - it never goes
    // through PaymentService, so it says nothing about our own findByIdemKey short-circuit.
    // see authorizeSkipsReprocessingOnDuplicateRequest below for that.
    @Test
    void bankLevelIdempotencyReturnsCachedResponse () throws Exception {
        OrderDetail cardDetail = new OrderDetail();
        cardDetail.setCardNumber(cardNumber);
        cardDetail.setCvv(cvv);
        cardDetail.setExpiryMonth(expiryMonth);
        cardDetail.setExpiryYear(expiryYear);

        BankAuthResponse response1 = bankClient.postAuthorization(cardDetail, amount, orderId, paymentRef, idemKey);
        BankAuthResponse response2 = bankClient.postAuthorization(cardDetail, amount, orderId, paymentRef, idemKey);

        assertEquals(response1.getCurrentState(), response2.getCurrentState());
        assertEquals(response1.getAuthorizationId(), response2.getAuthorizationId());
        assertEquals(response1.getAmount(), response2.getAmount());
    }

    // PaymentService.authorizePayment checks findByIdemKey before ever calling the bank
    @Test
    void authorizeSkipsReprocessingOnDuplicateRequest () throws Exception {
        gatewayApis.authorize(buildOrderDetail());
        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();

        ResponseEntity<String> secondResponse = gatewayApis.authorize(buildOrderDetail());

        assertEquals("This order has already been processed" + ".\n Please check you details.", secondResponse.getBody());

        long approvedEventCount = paymentEventRepository.findByPaymentRef(paymentRef).stream()
            .filter(e -> e.getCurrentState() == State.APPROVED)
            .count();
        assertEquals(1, approvedEventCount);
    }

   @Test
    void authorizeSavesCorrectly () throws Exception {

        gatewayApis.authorize(buildOrderDetail());

        assertEquals(State.APPROVED, paymentRepository.findByOrderId(orderId).getCurrentState());

        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();
        String approvedAt = paymentEventRepository.findByPaymentRef(paymentRef).get(paymentEventRepository.findByPaymentRef(paymentRef).size() - 1).getTimeCreated();
        System.out.print(approvedAt);
        System.out.print(paymentRepository.findByOrderId(orderId).getCreatedAt());
        assertEquals(paymentRepository.findByOrderId(orderId).getCreatedAt(), approvedAt);
    }

    @Test
    void flagIncorrectCardDetails () throws Exception {
        cardNumber = "411111111111111";

        assertThrows(WrongCardDetail.class, () -> gatewayApis.authorize(buildOrderDetail()));
    }

    @Test
    void captureRefusesVoidedTxns () throws Exception {

        gatewayApis.authorize(buildOrderDetail());

        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();

        gatewayApis.voids(paymentRef);

        assertThrows(WrongTransactionType.class, () -> gatewayApis.capture(paymentRef));
    }

    @Test
    void voidRefusesCapturedTxns () throws Exception {

        amount = 2000;
        customerId = "cus002";
        orderId = "ord002";

        gatewayApis.authorize(buildOrderDetail());

        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();

        gatewayApis.capture(paymentRef);

        assertThrows(WrongTransactionType.class, () -> gatewayApis.voids(paymentRef));

    }

    @Test
    void paymentEventRetainsTheNoteItWasGiven () {
        PaymentEvent captureEvent = PaymentMapping.createPaymentEvent(
            "cap_123", "idem-abc", State.CAPTURED, "pay_ref", "2026-01-01T00:00:00", "Capture successful");
        assertEquals("Capture successful", captureEvent.getNotes());

        PaymentEvent voidEvent = PaymentMapping.createPaymentEvent(
            "void_123", "idem-def", State.VOIDED, "pay_ref", "2026-01-01T00:00:00", "Void successfully");
        assertEquals("Void successfully", voidEvent.getNotes());

        PaymentEvent refundEvent = PaymentMapping.createPaymentEvent(
            "ref_123", "idem-ghi", State.REFUNDED, "pay_ref", "2026-01-01T00:00:00", "Refund completed");
        assertEquals("Refund completed", refundEvent.getNotes());
    }

    // make sure Receipt doesn't do integer division (order.getAmount()/100) before assigning to a double, truncating cents. 1050 cents must render as 10.5, not 10
    @Test
    void receiptFormatsFractionalCentsCorrectly () throws Exception {
        String centsOrderId = "cents-precision-test";
        paymentRepository.save(PaymentMapping.createPaymentDetail("pay_centstest", centsOrderId, "cust-cents", 1050));

        String receiptText = receipt.createReceipt(centsOrderId);

        assertTrue(receiptText.contains("10.5"), "Expected receipt to show 10.5, got: " + receiptText);
    }

    // in case capture/void/refund call .get(size() - 1) on an empty event list
    // should give a clear 404 instead of throwing IndexOutOfBoundsException
    @Test
    void captureVoidRefundRejectUnknownPaymentRef () {
        String unknownRef = "pay_does_not_exist";

        assertThrows(ResourceNotFound.class, () -> paymentService.capturePayment(unknownRef));
        assertThrows(ResourceNotFound.class, () -> paymentService.voidPayment(unknownRef));
        assertThrows(ResourceNotFound.class, () -> paymentService.refund(unknownRef));
    }

    // / make sure getbankid fails clearly if the event is not found instead of calling bank with null
    @Test
    void getBankTransactionIdRejectsUnmatchedState () throws Exception {
        orderId = "getauth-mismatch-test";
        customerId = "getauth-mismatch-cust";
        amount = 500;

        gatewayApis.authorize(buildOrderDetail());
        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();

        // never captured, so no CAPTURED event exists for this ref
        assertThrows(ResourceNotFound.class, () -> paymentService.getBankTransactionId(paymentRef, State.CAPTURED));
    }

    // check bank details before calling bank
    @Test
    void authorizeRejectsNonNumericCardNumber () {
        cardNumber = "411111111111111a";
        assertThrows(WrongCardDetail.class, () -> gatewayApis.authorize(buildOrderDetail()));
    }

    @Test
    void authorizeRejectsMalformedCvv () {
        cvv = "12";
        assertThrows(WrongCardDetail.class, () -> gatewayApis.authorize(buildOrderDetail()));
    }

    @Test
    void authorizeRejectsOutOfRangeExpiryMonth () {
        expiryMonth = 13;
        assertThrows(WrongCardDetail.class, () -> gatewayApis.authorize(buildOrderDetail()));
    }

    @Test
    void authorizeRejectsNonPositiveAmount () {
        amount = 0;
        assertThrows(InvalidOrderDetail.class, () -> gatewayApis.authorize(buildOrderDetail()));
    }

    // covers the capture and refund success paths end-to-end
    @Test
    void fullLifecycleCaptureThenRefundSucceeds () throws Exception {
        orderId = "lifecycle-test";
        customerId = "lifecycle-cust";
        amount = 750;

        gatewayApis.authorize(buildOrderDetail());
        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();
        assertEquals(State.APPROVED, paymentRepository.findByPaymentRef(paymentRef).getCurrentState());

        gatewayApis.capture(paymentRef);
        assertEquals(State.CAPTURED, paymentRepository.findByPaymentRef(paymentRef).getCurrentState());

        gatewayApis.refund(paymentRef);
        assertEquals(State.REFUNDED, paymentRepository.findByPaymentRef(paymentRef).getCurrentState());
    }

    // once a payment has moved past APPROVED, a repeat capture/void/refund is rejected by the state guard -
    // and not by the idempotency key check, since the state check runs first.
    @Test
    void repeatedCaptureIsRejectedOnceAlreadyCaptured () throws Exception {
        orderId = "repeat-capture-test";
        customerId = "repeat-capture-cust";
        amount = 600;

        gatewayApis.authorize(buildOrderDetail());
        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();

        gatewayApis.capture(paymentRef);
        assertThrows(WrongTransactionType.class, () -> gatewayApis.capture(paymentRef));

        long capturedEventCount = paymentEventRepository.findByPaymentRef(paymentRef).stream()
            .filter(e -> e.getCurrentState() == State.CAPTURED)
            .count();
        assertEquals(1, capturedEventCount);
    }

    @Test
    void repeatedVoidIsRejectedOnceAlreadyVoided () throws Exception {
        orderId = "repeat-void-test";
        customerId = "repeat-void-cust";
        amount = 650;

        gatewayApis.authorize(buildOrderDetail());
        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();

        gatewayApis.voids(paymentRef);
        assertThrows(WrongTransactionType.class, () -> gatewayApis.voids(paymentRef));

        long voidedEventCount = paymentEventRepository.findByPaymentRef(paymentRef).stream()
            .filter(e -> e.getCurrentState() == State.VOIDED)
            .count();
        assertEquals(1, voidedEventCount);
    }

    @Test
    void repeatedRefundIsRejectedOnceAlreadyRefunded () throws Exception {
        orderId = "repeat-refund-test";
        customerId = "repeat-refund-cust";
        amount = 700;

        gatewayApis.authorize(buildOrderDetail());
        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();

        gatewayApis.capture(paymentRef);
        gatewayApis.refund(paymentRef);
        assertThrows(WrongTransactionType.class, () -> gatewayApis.refund(paymentRef));

        long refundedEventCount = paymentEventRepository.findByPaymentRef(paymentRef).stream()
            .filter(e -> e.getCurrentState() == State.REFUNDED)
            .count();
        assertEquals(1, refundedEventCount);
    }

    @Test
    void getAuthorizationReturnsBankDetailForApprovedPayment () throws Exception {
        orderId = "get-auth-test";
        customerId = "get-auth-cust";
        amount = 300;

        gatewayApis.authorize(buildOrderDetail());
        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();

        BankAuthResponse response = gatewayApis.getAuthorization(paymentRef);

        assertEquals("APPROVED", response.getCurrentState().toUpperCase());
        assertEquals(paymentService.getBankTransactionId(paymentRef, State.APPROVED), response.getAuthorizationId());
    }

    @Test
    void getCaptureReturnsBankDetailForCapturedPayment () throws Exception {
        orderId = "get-capt-test";
        customerId = "get-capt-cust";
        amount = 350;

        gatewayApis.authorize(buildOrderDetail());
        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();
        gatewayApis.capture(paymentRef);

        BankCaptureResponse response = gatewayApis.getCapture(paymentRef);

        assertEquals("CAPTURED", response.getCurrentState().toUpperCase());
    }

    @Test
    void getRefundReturnsBankDetailForRefundedPayment () throws Exception {
        orderId = "get-ref-test";
        customerId = "get-ref-cust";
        amount = 400;

        gatewayApis.authorize(buildOrderDetail());
        String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();
        gatewayApis.capture(paymentRef);
        gatewayApis.refund(paymentRef);

        BankRefundResponse response = gatewayApis.getRefund(paymentRef);

        assertEquals("REFUNDED", response.getCurrentState().toUpperCase());
        assertEquals(paymentService.getBankTransactionId(paymentRef, State.REFUNDED), response.getRefundId());
    }

    @Test
    void getOrderStatusReturnsCurrentPaymentDetail () throws Exception {
        orderId = "order-status-test";
        customerId = "order-status-cust";
        amount = 450;

        gatewayApis.authorize(buildOrderDetail());

        PaymentDetail status = gatewayApis.getOrderStatus(orderId);

        assertEquals(State.APPROVED, status.getCurrentState());
        assertEquals(customerId, status.getCustomerId());
    }

    @Test
    void getOrdersByCustomerIdReturnsPaymentDetail () throws Exception {
        orderId = "orders-by-customer-test";
        customerId = "orders-by-customer-cust";
        amount = 480;

        gatewayApis.authorize(buildOrderDetail());

        PaymentDetail order = gatewayApis.getOrdersByCustomerId(customerId);

        assertEquals(orderId, order.getOrderId());
    }
}
