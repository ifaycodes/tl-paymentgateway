package com.gateway.gateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HexFormat;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.bankconnect.BankAuthResponse;
import com.gateway.bankconnect.BankCaptureResponse;
import com.gateway.bankconnect.BankClient;
import com.gateway.bankconnect.BankRefundResponse;
import com.gateway.bankconnect.BankVoidResponse;
import com.gateway.carddetails.CardDetail;
import com.gateway.data.PaymentRepository;
import com.gateway.models.OrderDetail;
import com.gateway.state.State;
import com.gateway.state.StateMachine;

@RestController
@RequestMapping("/api/v1")
public class GatewayApis {

    StateMachine stateMachine = new StateMachine();

    @Autowired
    private BankClient bankClient;

    @Autowired
    private PaymentRepository paymentRepository;

    OrderDetail orderDetail = new OrderDetail();

    public GatewayApis(DataSource dataSource, PaymentRepository paymentRepository, BankClient bankClient) {
        this.paymentRepository = paymentRepository;
        this.bankClient = bankClient;
    }

    @PostMapping("/authorize")
    public String authorize(@RequestParam int amount, @RequestParam String orderId, @RequestParam String customerId, @RequestParam String cardNumber, @RequestParam String cvv, @RequestParam int expiryMonth, @RequestParam int expiryYear) throws Exception {
        CardDetail cardDetail = new CardDetail();
        cardDetail.setCardNumber(cardNumber);
        cardDetail.setCvv(cvv);
        cardDetail.setExpiryMonth(expiryMonth);
        cardDetail.setExpiryYear(expiryYear);

        BankAuthResponse bankResponse = bankClient.authorization(cardDetail, amount);

        String authorization_id = bankResponse.getAuthorizationId();

        String paymentRef = generatePaymentRef(authorization_id, orderId, customerId);

        orderDetail.setPaymentRef(paymentRef);
        orderDetail.setAuthorizationId(authorization_id);
        orderDetail.setAmount(amount);
        orderDetail.setCustomerId(customerId);
        orderDetail.setOrderId(orderId);
        orderDetail.setCurrentState(State.APPROVED);
        orderDetail.setCreatedAt(bankResponse.getCreatedAt());
        
        paymentRepository.save(orderDetail);
        return paymentRef;
    }

    @PostMapping("/capture")
    public String capture(@RequestParam String paymentRef) throws Exception {

        String authorizationId = paymentRepository.findByPaymentRef(paymentRef).getAuthorizationId();
        int amount = paymentRepository.findByPaymentRef(paymentRef).getAmount();

        //System.out.print(authorizationId + " " + amount);

        BankCaptureResponse captureResponse = bankClient.capture(amount, authorizationId);
        String captureId = captureResponse.getCaptureId();

        paymentRepository.updateStatus(paymentRef, State.CAPTURED);
        paymentRepository.updateCapture(paymentRef, captureId, captureResponse.getCapturedAt());
        String confirmation = "This order has been captured. Capture ID: " + captureId;
        return confirmation;
    }

    @PostMapping("/void")
    public String voids(@RequestParam String paymentRef) throws Exception {
        String authorizationId = paymentRepository.findByPaymentRef(paymentRef).getAuthorizationId();

        BankVoidResponse voidResponse = bankClient.voidOrder(authorizationId);
        String voidId = voidResponse.getVoidId();

        paymentRepository.updateStatus(paymentRef, State.VOIDED);
        paymentRepository.updateVoid(paymentRef, voidId, voidResponse.getVoidedAt());
        String confirmation = "This order has been voided. Capture ID: " + voidId;
        return confirmation;
        
    }

    @PostMapping("/refund")
    public String refund(@RequestParam String paymentRef) throws Exception {
        String captureId = paymentRepository.findByPaymentRef(paymentRef).getCaptureId();
        int amount = paymentRepository.findByPaymentRef(paymentRef).getAmount();

        BankRefundResponse refundResponse = bankClient.refund(amount, captureId);
        String refundId = refundResponse.getRefundId();

        paymentRepository.updateStatus(paymentRef, State.REFUNDED);
        paymentRepository.updateRefund(paymentRef, refundId, refundResponse.getRefundedAt());
        String confirmation = "This order has been voided. Refund ID: " + refundId;
        return confirmation;
        
    }

    @GetMapping("/orders")
    public OrderDetail getOrdersByCustomerId(String customerId) throws SQLException {
        return paymentRepository.findByCustomerId(customerId);
    }

    @GetMapping("/status")
    public State getOrderStatus(String orderId) throws SQLException {
        return paymentRepository.findByOrderId(orderId);
    }


    // method to generate a payment key using hash
    public String generatePaymentRef(String authorization_id, String orderId, String customerId) throws NoSuchAlgorithmException {
    String raw = authorization_id + orderId + customerId;
    
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
    
    // we take first 16 chars to keep it short
    return "pay_" + HexFormat.of().formatHex(hash).substring(0, 16);
    }

}
