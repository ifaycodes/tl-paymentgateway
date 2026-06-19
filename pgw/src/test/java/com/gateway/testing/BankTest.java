package com.gateway.testing;

import com.gateway.bankconnectresponses.BankAuthResponse;
import com.gateway.data.AuditRepository;
import com.gateway.data.PaymentEventRepository;
import com.gateway.data.PaymentRepository;
import com.gateway.gateway.*;
import com.gateway.models.CardDetail;
import com.gateway.pgw.PgwApplication;
import com.gateway.proof.Receipt;
import com.gateway.state.State;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

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

    private Receipt receipt;

    @Autowired
    private BankClient bankClient;
    private DataSource dataSource;

    @Autowired
    private AuditRepository auditRepository;
    
    @Autowired
    private GatewayApis gatewayApis;

    @BeforeEach
    void setUp() {
        bankClient = new BankClient(auditRepository, dataSource);
        gatewayApis = new GatewayApis(dataSource, paymentRepository, paymentEventRepository, bankClient, receipt);

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


    @Test
    void respectsIdempotency () throws Exception {
        CardDetail cardDetail = new CardDetail();
        cardDetail.setCardNumber(cardNumber);
        cardDetail.setCvv(cvv);
        cardDetail.setExpiryMonth(expiryMonth);
        cardDetail.setExpiryYear(expiryYear);

        BankAuthResponse response1 = bankClient.postAuthorization(cardDetail, amount, orderId, paymentRef, idemKey);
        BankAuthResponse response2 = bankClient.postAuthorization(cardDetail, amount, orderId, paymentRef, idemKey);

        assertEquals(response1.getCurrentState(), response2.getCurrentState());
        assertEquals(response1.getAuthorizationId(), response2.getAuthorizationId());
        
    }

   @Test
    void authorizeSavesCorrectly () throws Exception {

        gatewayApis.authorize(amount, orderId, customerId, cardNumber, cvv, expiryMonth, expiryYear);

        //assertNotNull(paymentRepository.findByPaymentRef(paymentRef));
        assertEquals(State.APPROVED, paymentRepository.findByOrderId(orderId).getCurrentState());
        
        //String paymentRef = paymentRepository.findByOrderId(orderId).getPaymentRef();
        //String approvedAt = paymentEventRepository.findByPaymentRef(paymentRef).get(paymentEventRepository.findByPaymentRef(paymentRef).size() - 1).getTimeCreated();
        //System.out.print(approvedAt);
        //System.out.print(paymentRepository.findByOrderId(orderId).getCreatedAt());
        //assertNotEquals(paymentRepository.findByOrderId(orderId).getCreatedAt(), approvedAt);
    }
}
