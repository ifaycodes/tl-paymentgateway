package com.gateway.gateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.bankconnectresponses.BankAuthResponse;
import com.gateway.bankconnectresponses.BankCaptureResponse;
import com.gateway.bankconnectresponses.BankRefundResponse;
import com.gateway.bankconnectresponses.BankVoidResponse;
import com.gateway.data.PaymentEventRepository;
import com.gateway.data.PaymentRepository;
import com.gateway.errors.BankNotConnectingException;
import com.gateway.errors.BankPermanentError;
import com.gateway.models.CardDetail;
import com.gateway.models.PaymentDetail;
import com.gateway.models.PaymentEvent;
import com.gateway.proof.Receipt;
import com.gateway.state.State;

@RestController
@RequestMapping("/api/v1")
public class GatewayApis {


    @Autowired
    private Receipt receipt;

    @Autowired
    private BankClient bankClient;

    @Autowired
    private PaymentRepository paymentRepository;


    @Autowired
    private PaymentEventRepository paymentEventRepository;

    int attempts = 0;

    PaymentDetail paymentDetail = new PaymentDetail();
    PaymentEvent paymentEvent = new PaymentEvent();

    //constructor
    public GatewayApis(DataSource dataSource, PaymentRepository paymentRepository, PaymentEventRepository paymentEventRepository, BankClient bankClient, Receipt receipt) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.bankClient = bankClient;
        this.receipt = receipt;
    }

    // api mapping
    @PostMapping("/authorize")
    public String authorize(@RequestParam int amount, @RequestParam String orderId, @RequestParam String customerId, @RequestParam String cardNumber, @RequestParam String cvv, @RequestParam int expiryMonth, @RequestParam int expiryYear) throws Exception {
        CardDetail cardDetail = new CardDetail();
        cardDetail.setCardNumber(cardNumber);
        cardDetail.setCvv(cvv);
        cardDetail.setExpiryMonth(expiryMonth);
        cardDetail.setExpiryYear(expiryYear);


        String id = orderId + amount;
        UUID idempotencyKey = UUID.nameUUIDFromBytes(id.getBytes());

        String paymentRef = generatePaymentRef(orderId, customerId);


        //check if idempotencyKey already exist in db. if it does, no need to do save again and call bank
        if (!paymentEventRepository.findByIdemKey(idempotencyKey.toString())) {

            // populate the payment details and save to db so to save the information before calling bank
            if (paymentRepository.findByPaymentRef(paymentRef) == null) {
                paymentDetail.setPaymentRef(paymentRef);
                paymentDetail.setOrderId(orderId);
                paymentDetail.setCustomerId(customerId);
                paymentDetail.setAmount(amount);
                paymentDetail.setCurrentState(State.PENDING);
                paymentDetail.setCreatedAt(LocalDateTime.now().toString());

                paymentRepository.save(paymentDetail);
            }

            while (attempts < 3) {
                try {
                    // calling bank api
                    BankAuthResponse bankResponse = bankClient.postAuthorization(cardDetail, amount, orderId, paymentRef);
                    
                    // update payment status
                    paymentRepository.updateAuthorization(paymentRef, bankResponse.getCurrentState(), bankResponse.getCreatedAt());

                    String authorizationId = bankResponse.getAuthorizationId();

                    // populate payment event so it can be saved for querying
                    paymentEvent.setBankTransactionId(authorizationId);
                    paymentEvent.setIdempotencyKey(idempotencyKey.toString());
                    paymentEvent.setCurrentState(State.valueOf(bankResponse.getCurrentState().toUpperCase()));
                    paymentEvent.setPaymentRef(paymentRef);
                    paymentEvent.setTimeCreated(bankResponse.getCreatedAt());
                    paymentEvent.setNotes("Authorization successful");
                    paymentEventRepository.save(paymentEvent);
                    
                    // return confirmation of transaction to ficmart
                    String confirmation = "This order has been approved. Authorization ID: " + authorizationId + ".\n Payment Ref: " + paymentRef;
                    return confirmation;

                } catch (BankNotConnectingException e) {
                    attempts++;
                    if (attempts == 3) throw e;
                    Thread.sleep(1000 * attempts);

                } catch (BankPermanentError e) {
                    paymentRepository.updateStatus(paymentRef, State.FAILED);
                    throw e;
                }
            }
            
        } 

        // should the bank timeout before db update is done, calling authorize again should only update status of the txn is need be, else return
        if (paymentRepository.findByPaymentRef(paymentRef).getCurrentState() != State.APPROVED) {
                paymentRepository.updateStatus(paymentRef, State.APPROVED);
            }
        
        return ("This order has already been processed" + ".\n Please check you details.");
          
    }

    //get auth with id
    @GetMapping("/getauth")
    public BankAuthResponse authorize(String paymentRef) throws Exception {
        String authorizationId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);
        for (PaymentEvent event : eventAssociatedWithRef) {
            authorizationId = event.getBankTransactionId();
            break;
        }

        BankAuthResponse bankAuthResponse = bankClient.getAuthorization(authorizationId, paymentRef);
        return bankAuthResponse;
    }
     
    //send capture request
    @PostMapping("/capture")
    public String capture(@RequestParam String paymentRef) throws Exception {

        String authorizationId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);
        PaymentEvent lastEvent = eventAssociatedWithRef.get(eventAssociatedWithRef.size() - 1);
        
        if (lastEvent.getCurrentState() == State.APPROVED) {
            authorizationId = lastEvent.getBankTransactionId();
            //break;
        } else {
            return "This transaction CAN NOT be Captured. It has been Voided";
        }
        

        int amount = paymentRepository.findByPaymentRef(paymentRef).getAmount();

        String id = authorizationId + amount;
        UUID idempotencyKey = UUID.nameUUIDFromBytes(id.getBytes());

        if (!paymentEventRepository.findByIdemKey(idempotencyKey.toString())) {

            while (attempts < 3) {

                try {
                    BankCaptureResponse captureResponse = bankClient.postCapture(amount, authorizationId, paymentRef);
                
                    String captureId = captureResponse.getCaptureId();

                    // update payment status
                    paymentRepository.updateStatus(paymentRef, State.CAPTURED);

                    // populate payment event so it can be saved for querying
                    paymentEvent.setBankTransactionId(captureId);
                    paymentEvent.setIdempotencyKey(idempotencyKey.toString());
                    paymentEvent.setCurrentState(State.valueOf(captureResponse.getCurrentState().toUpperCase()));
                    paymentEvent.setPaymentRef(paymentRef);
                    paymentEvent.setTimeCreated(captureResponse.getCapturedAt());
                    paymentEvent.setNotes("Capture successful");
                    paymentEventRepository.save(paymentEvent);

                    String confirmation = "This order has been captured. Capture ID: " + captureId + ".\n Product will be shipped soon!";
                    return confirmation;

                } catch (BankNotConnectingException e) {
                    attempts++;
                    if (attempts == 3) throw e;
                    Thread.sleep(1000 * attempts);

                } catch (BankPermanentError e) {
                    paymentRepository.updateStatus(paymentRef, State.FAILED);
                    throw e;
                }
            }
        }
        return ("This payment has already been captured.");
    }

    // return bank capture response
    @GetMapping("/getcapture")
    public BankCaptureResponse getCapture(String paymentRef) throws Exception {
        String captureId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);
        for (PaymentEvent event : eventAssociatedWithRef) {
            if (event.getCurrentState() == State.CAPTURED) {
                captureId = event.getBankTransactionId();
                break;
            }
            break;
        }

        BankCaptureResponse captureResponse = bankClient.getCapture(captureId, paymentRef);
        return captureResponse;
    }

    @PostMapping("/void")
    public String voids(@RequestParam String paymentRef) throws Exception {

        String authorizationId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);

        for (PaymentEvent event : eventAssociatedWithRef) {
            if (event.getCurrentState() == State.APPROVED) {
                authorizationId = event.getBankTransactionId();
                break;
            }
        }
        System.out.println("auth id is : " + authorizationId);

        UUID idempotencyKey = UUID.nameUUIDFromBytes(authorizationId.getBytes());
        
        if (!paymentEventRepository.findByIdemKey(idempotencyKey.toString())) {

            while (attempts < 3) {
                try {
            
                    BankVoidResponse voidResponse = bankClient.postVoid(authorizationId, paymentRef);
                    String voidId = voidResponse.getVoidId();

                    paymentRepository.updateStatus(paymentRef, State.VOIDED);
                    
                    // populate payment event so it can be saved for querying
                    paymentEvent.setBankTransactionId(voidId);
                    paymentEvent.setIdempotencyKey(idempotencyKey.toString());
                    paymentEvent.setCurrentState(State.valueOf(voidResponse.getCurrentState().toUpperCase()));
                    paymentEvent.setPaymentRef(paymentRef);
                    paymentEvent.setTimeCreated(voidResponse.getVoidedAt());
                    paymentEvent.setNotes("Void successful");
                    paymentEventRepository.save(paymentEvent);

                    String confirmation = "This order has been voided. Void ID: " + voidId + "\n Product will not be shipped";
                    return confirmation;

                } catch (BankNotConnectingException e) {
                    attempts++;
                    if (attempts == 3) throw e;
                    Thread.sleep(1000 * attempts);

                } catch (BankPermanentError e) {
                    paymentRepository.updateStatus(paymentRef, State.FAILED);
                    throw e;
                }
            }
        }

        return "You can only void an authorized transaction!";
        
    }

    @PostMapping("/refund")
    public String refund(@RequestParam String paymentRef) throws Exception {
        
        int amount = paymentRepository.findByPaymentRef(paymentRef).getAmount();
        String captureId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);
        for (PaymentEvent event : eventAssociatedWithRef) {
            if (event.getCurrentState() == State.CAPTURED) {
                captureId = event.getBankTransactionId();
                break;
            }
        }

        String id = amount + captureId;
        UUID idempotencyKey = UUID.nameUUIDFromBytes(id.getBytes());
        
        if (!paymentEventRepository.findByIdemKey(idempotencyKey.toString())) {

            while (attempts < 3) {
                try {
                    BankRefundResponse refundResponse = bankClient.postRefund(amount, captureId, paymentRef);
                    String refundId = refundResponse.getRefundId();

                    paymentRepository.updateStatus(paymentRef, State.REFUNDED);
                    
                    // populate payment event so it can be saved for querying
                    paymentEvent.setBankTransactionId(refundId);
                    paymentEvent.setIdempotencyKey(idempotencyKey.toString());
                    paymentEvent.setCurrentState(State.valueOf(refundResponse.getCurrentState().toUpperCase()));
                    paymentEvent.setPaymentRef(paymentRef);
                    paymentEvent.setTimeCreated(refundResponse.getRefundedAt());
                    paymentEvent.setNotes("Refund successful");
                    paymentEventRepository.save(paymentEvent);

                    String confirmation = "This payment has been refunded. Refund ID: " + refundId + "\n Product will not be shipped";
                    return confirmation;

                } catch (BankNotConnectingException e) {
                    attempts++;
                    if (attempts == 3) throw e;
                    Thread.sleep(1000 * attempts);

                } catch (BankPermanentError e) {
                    paymentRepository.updateStatus(paymentRef, State.FAILED);
                    throw e;
                }
            }
        }

        return "You can only refund a captured transaction!";
        
    }

    @GetMapping("/getrefund")
    public BankRefundResponse getRefund(String paymentRef) throws Exception {
        String refundId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);
        for (PaymentEvent event : eventAssociatedWithRef) {
            if (event.getCurrentState() == State.CAPTURED) {
                refundId = event.getBankTransactionId();
                break;
            }
            break;
        }

        BankRefundResponse refundResponse = bankClient.getRefund(refundId, paymentRef);
        return refundResponse;
    }

    @PostMapping("/receipts")
    public String receipts(@RequestParam String orderId) throws SQLException {
        try {
            String orderReceipt = receipt.createReceipt(orderId);

            return orderReceipt;

        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/orders")
    public PaymentDetail getOrdersByCustomerId(@RequestParam String customerId) throws SQLException {
        return paymentRepository.findByCustomerId(customerId);
    }

    @GetMapping("/status")
    public PaymentDetail getOrderStatus(@RequestParam String orderId) throws SQLException {
        return paymentRepository.findByOrderId(orderId);
    }


    // method to generate a payment key using hash
    public String generatePaymentRef(String orderId, String customerId) throws NoSuchAlgorithmException {
        String placeHolder = "placeholder";
        String raw = placeHolder + orderId + customerId;
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        
        // we take first 16 chars to keep it short
        return "pay_" + HexFormat.of().formatHex(hash).substring(0, 18);
    }

}
