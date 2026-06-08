package com.gateway.gateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    private final DataSource dataSource = null;
    StateMachine stateMachine = new StateMachine();
    BankClient bankClient = new BankClient();
    PaymentRepository paymentRepository = new PaymentRepository(dataSource);
    OrderDetail orderDetail = new OrderDetail();

    @PostMapping()
    public String authorize(int amount, String orderId, String customerId, CardDetail cardDetail) throws Exception {
        
        BankAuthResponse bankResponse = bankClient.authorization(cardDetail, amount);

        String authorizationId = bankResponse.getAuthorizationId();

        String paymentRef = generatePaymentRef(authorizationId, orderId, customerId);

        orderDetail.setPaymentRef(paymentRef);
        orderDetail.setAuthorizationId(authorizationId);
        orderDetail.setAmount(amount);
        orderDetail.setCustomerId(customerId);
        orderDetail.setOrderId(orderId);
        orderDetail.setCurrentState(State.AUTHORIZED);
        orderDetail.setCreatedAt(LocalDateTime.now());
        
        paymentRepository.save(orderDetail);
        return paymentRef;
    }

    @PostMapping()
    public String capture(String paymentRef) throws Exception {
        BankCaptureResponse captureResponse = bankClient.capture(paymentRef);
        String captureId = captureResponse.getCaptureId();

        paymentRepository.updateStatus(paymentRef, State.CAPTURED);
        paymentRepository.updateCapture(paymentRef, captureId, captureResponse.getCapturedAt());
        String confirmation = "This order has been captured. Capture ID: " + captureId;
        return confirmation;
    }

    @PostMapping
    public String voids(String paymentRef) throws Exception {
        BankVoidResponse voidResponse = bankClient.voidOrder(paymentRef);
        String voidId = voidResponse.getVoidId();

        paymentRepository.updateStatus(paymentRef, State.VOIDED);
        paymentRepository.updateVoid(paymentRef, voidId, voidResponse.getVoidedAt());
        String confirmation = "This order has been voided. Capture ID: " + voidId;
        return confirmation;
        
    }

    @PostMapping
    public String refund(String paymentRef) throws Exception {
        String captureId = paymentRepository.findByPaymentRef(paymentRef).getCaptureId();

        BankRefundResponse refundResponse = bankClient.refund(captureId);
        String refundId = refundResponse.getRefundId();

        paymentRepository.updateStatus(paymentRef, State.REFUNDED);
        paymentRepository.updateRefund(paymentRef, refundId, refundResponse.getRefundedAt());
        String confirmation = "This order has been voided. Refund ID: " + refundId;
        return confirmation;
        
    }

    @GetMapping
    public OrderDetail getOrdersByCustomerId(String customerId) throws SQLException {
        return paymentRepository.findByCustomerId(customerId);
    }

    @GetMapping
    public State getOrderStatus(String orderId) throws SQLException {
        return paymentRepository.findByOrderId(orderId);
    }


    // method to generate a payment key using hash
    public String generatePaymentRef(String authorizationId, String orderId, String customerId) throws NoSuchAlgorithmException {
    String raw = authorizationId + orderId + customerId;
    
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
    
    // we take first 16 chars to keep it short
    return "pay_" + HexFormat.of().formatHex(hash).substring(0, 16);
}

}
