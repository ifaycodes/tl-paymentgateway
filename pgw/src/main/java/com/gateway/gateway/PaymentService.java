package com.gateway.gateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.gateway.bankconnectresponses.BankAuthResponse;
import com.gateway.bankconnectresponses.BankCaptureResponse;
import com.gateway.bankconnectresponses.BankRefundResponse;
import com.gateway.bankconnectresponses.BankVoidResponse;
import com.gateway.data.PaymentEventRepository;
import com.gateway.data.PaymentRepository;
import com.gateway.errors.BankNotConnectingException;
import com.gateway.errors.BankPermanentError;
import com.gateway.errors.InvalidOrderDetail;
import com.gateway.errors.ResourceNotFound;
import com.gateway.errors.WrongCardDetail;
import com.gateway.errors.WrongTransactionType;
import com.gateway.models.OrderDetail;
import com.gateway.models.PaymentDetail;
import com.gateway.models.PaymentEvent;
import com.gateway.state.State;

@Service
public class PaymentService {

    private final BankClient bankClient;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventRepository paymentEventRepository, BankClient bankClient) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.bankClient = bankClient;
    }

    // authorize an order payment
    public String authorizePayment(OrderDetail orderDetail) throws Exception {
        validateOrderDetail(orderDetail);

        int amount = orderDetail.getAmt() * 100;
        String orderId = orderDetail.getOrderId();
        String customerId = orderDetail.getCustomerId();

        String id = orderId + amount;
        String idempotencyKey = UUID.nameUUIDFromBytes(id.getBytes()).toString();

        String paymentRef = generatePaymentRef(orderId, customerId);


        //check if idempotencyKey already exist in db. if it does, no need to do save again and call bank
        if (!paymentEventRepository.findByIdemKey(idempotencyKey)) {

            // populate the payment details and save to db so to save the information before calling bank
            if (paymentRepository.findByPaymentRef(paymentRef) == null) {
               PaymentDetail paymentDetail = new PaymentDetail();
               paymentDetail = PaymentMapping.createPaymentDetail(paymentRef, orderId, customerId, amount);
               paymentRepository.save(paymentDetail);
            }

            int attempts = 0;
            while (attempts < 3) {
                try {
                    // calling bank api
                    BankAuthResponse bankResponse = bankClient.postAuthorization(orderDetail, amount, orderId, paymentRef, idempotencyKey);
                    
                    // update payment status
                    paymentRepository.updateAuthorization(paymentRef, bankResponse.getCurrentState(), bankResponse.getCreatedAt());

                    String authorizationId = bankResponse.getAuthorizationId();
                    String note = "Authorization successful";

                    // populate payment event so it can be saved for querying
                    PaymentEvent paymentEvent = new PaymentEvent();
                    paymentEvent = PaymentMapping.createPaymentEvent(authorizationId, idempotencyKey, 
                        State.valueOf(bankResponse.getCurrentState().toUpperCase()), paymentRef, bankResponse.getCreatedAt(), note);
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

    // capture authorized payment
    public String capturePayment(String paymentRef) throws Exception {

        String authorizationId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);
        if (eventAssociatedWithRef.isEmpty()) {
            throw new ResourceNotFound("No payment found for this reference");
        }
        PaymentEvent lastEvent = eventAssociatedWithRef.get(eventAssociatedWithRef.size() - 1);

        if (lastEvent.getCurrentState() == State.APPROVED) {
            authorizationId = lastEvent.getBankTransactionId();
            //break;
        } else {
            throw new WrongTransactionType("This transaction CAN NOT be Captured.");
        }

        int amount = paymentRepository.findByPaymentRef(paymentRef).getAmount();

        String id = authorizationId + amount;
        String idempotencyKey = UUID.nameUUIDFromBytes(id.getBytes()).toString();

        if (!paymentEventRepository.findByIdemKey(idempotencyKey)) {

            int attempts = 0;
            while (attempts < 3) {

                try {
                    BankCaptureResponse captureResponse = bankClient.postCapture(amount, authorizationId, paymentRef, idempotencyKey);
                
                    String captureId = captureResponse.getCaptureId();

                    // update payment status
                    paymentRepository.updateStatus(paymentRef, State.CAPTURED);
                    String note = "Capture successful";

                    // populate payment event so it can be saved for querying
                    PaymentEvent paymentEvent = new PaymentEvent();
                    paymentEvent = PaymentMapping.createPaymentEvent(captureId, idempotencyKey.toString(), 
                        State.valueOf(captureResponse.getCurrentState().toUpperCase()), paymentRef, captureResponse.getCapturedAt(), note);
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

    // void an authorized payment
    public String voidPayment(String paymentRef) throws Exception {

        String authorizationId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);
        if (eventAssociatedWithRef.isEmpty()) {
            throw new ResourceNotFound("No payment found for this reference");
        }
        PaymentEvent lastEvent = eventAssociatedWithRef.get(eventAssociatedWithRef.size() - 1);

        if (lastEvent.getCurrentState() == State.APPROVED) {
            authorizationId = lastEvent.getBankTransactionId();
            //break;
        } else {
            throw new WrongTransactionType ("This transaction CAN NOT be Voided. Please try asking for a refund instead");
        }

        String idempotencyKey = (UUID.nameUUIDFromBytes(authorizationId.getBytes())).toString();
        
        if (!paymentEventRepository.findByIdemKey(idempotencyKey)) {

            int attempts = 0;
            while (attempts < 3) {
                try {
            
                    BankVoidResponse voidResponse = bankClient.postVoid(authorizationId, paymentRef, idempotencyKey);
                    String voidId = voidResponse.getVoidId();

                    paymentRepository.updateStatus(paymentRef, State.VOIDED);
                    String note = "Void successfully";
                    
                    // populate payment event so it can be saved for querying
                    PaymentEvent paymentEvent = new PaymentEvent();
                    paymentEvent = PaymentMapping.createPaymentEvent(voidId, idempotencyKey, 
                        State.valueOf(voidResponse.getCurrentState().toUpperCase()), paymentRef, voidResponse.getVoidedAt(), note);
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

    // refund a captured payment
    public String refund(@RequestParam String paymentRef) throws Exception {

        String captureId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);
        if (eventAssociatedWithRef.isEmpty()) {
            throw new ResourceNotFound("No payment found for this reference");
        }
        PaymentEvent lastEvent = eventAssociatedWithRef.get(eventAssociatedWithRef.size() - 1);

        if (lastEvent.getCurrentState() == State.CAPTURED) {
            captureId = lastEvent.getBankTransactionId();
            //break;
        } else {
            throw new WrongTransactionType ("This transaction CAN NOT be Refunded. It was Voided. Void ID: " + lastEvent.getBankTransactionId());
        }

        // the payments row is guaranteed to exist here: authorizePayment always creates it
        // before any event is written, and we've already confirmed an event exists above
        int amount = paymentRepository.findByPaymentRef(paymentRef).getAmount();

        String id = amount + captureId;
        String idempotencyKey = (UUID.nameUUIDFromBytes(id.getBytes())).toString();
        
        if (!paymentEventRepository.findByIdemKey(idempotencyKey)) {

            int attempts = 0;
            while (attempts < 3) {
                try {
                    BankRefundResponse refundResponse = bankClient.postRefund(amount, captureId, paymentRef, idempotencyKey);
                    String refundId = refundResponse.getRefundId();

                    paymentRepository.updateStatus(paymentRef, State.REFUNDED);
                    String note = "Refund completed";
                    
                    // populate payment event so it can be saved for querying
                    PaymentEvent paymentEvent = new PaymentEvent();
                    paymentEvent = PaymentMapping.createPaymentEvent(refundId, idempotencyKey, 
                        State.valueOf(refundResponse.getCurrentState().toUpperCase()), paymentRef, refundResponse.getRefundedAt(), note);
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

    // return bank transaction id for different state of txns using payment ref
    public String getBankTransactionId(String paymentRef, State status) throws Exception {
        String bankTransactionId = null;
        List<PaymentEvent> eventAssociatedWithRef = paymentEventRepository.findByPaymentRef(paymentRef);
        for (PaymentEvent event : eventAssociatedWithRef) {
            if (event.getCurrentState().equals(status)) {
                bankTransactionId = event.getBankTransactionId();
                break;
            }
        }
        if (bankTransactionId == null) {
            throw new ResourceNotFound("No " + status + " transaction found for this payment reference");
        }
        return bankTransactionId;
    }

    // validate order/card details before authorizing with the bank
    private void validateOrderDetail(OrderDetail orderDetail) {
        String cardNumber = orderDetail.getCardNumber();
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            throw new WrongCardDetail("Check card detail");
        }

        String cvv = orderDetail.getCvv();
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new WrongCardDetail("Check card detail");
        }

        if (orderDetail.getExpiryMonth() < 1 || orderDetail.getExpiryMonth() > 12) {
            throw new WrongCardDetail("Check card detail");
        }

        if (orderDetail.getAmt() <= 0) {
            throw new InvalidOrderDetail("Amount must be greater than zero");
        }
    }

    // generate paymentref using hash
    public String generatePaymentRef(String orderId, String customerId) throws NoSuchAlgorithmException {
        String placeHolder = "placeholder";
        String raw = placeHolder + orderId + customerId;
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        
        // we take first 16 chars to keep it short
        return "pay_" + HexFormat.of().formatHex(hash).substring(0, 18);
    }
}
