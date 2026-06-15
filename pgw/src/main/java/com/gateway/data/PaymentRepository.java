package com.gateway.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.gateway.models.PaymentDetail;
import com.gateway.state.State;

@Repository
public class PaymentRepository {

    private final DataSource dataSource;

    public PaymentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // to add an order payment to the database
    public void save(PaymentDetail paymentDetail) throws SQLException {
        String sql = """
                INSERT INTO payments (paymentRef, orderId, customerId, amount, currentState, currency, createdAt)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setString(1, paymentDetail.getPaymentRef());
                    statement.setString(2, paymentDetail.getOrderId());
                    statement.setString(3, paymentDetail.getCustomerId());
                    statement.setInt(4, paymentDetail.getAmount());
                    statement.setString(5, paymentDetail.getCurrentState().toString());
                    statement.setString(6, paymentDetail.getCurrency());
                    statement.setString(7, paymentDetail.getCreatedAt().toString());

                    statement.executeUpdate();
                }

    }


    // to find an order in the database using the paymentRef
    public PaymentDetail findByPaymentRef(String paymentRef) throws SQLException {
        String sql = "SELECT * FROM payments WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, paymentRef);

        ResultSet result = statement.executeQuery();

        if (result.next()) {
                PaymentDetail payment = new PaymentDetail();
                payment.setPaymentRef(result.getString("paymentRef"));
                payment.setOrderId(result.getString("orderId"));
                payment.setCustomerId(result.getString("customerId"));
                payment.setAmount(result.getInt("amount"));
                payment.setCurrentState(State.valueOf(result.getString("currentState")));
                payment.setCreatedAt(LocalDateTime.parse(result.getString("createdAt")));
                return payment;
            }

            return null;
    }
    }

    
    // to see all orders by the customer with that customerid
    public PaymentDetail findByCustomerId(String customerId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE customerId = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, customerId);

        ResultSet result = statement.executeQuery();

        if (result.next()) {
                PaymentDetail payment = new PaymentDetail();
                payment.setPaymentRef(result.getString("paymentRef"));
                payment.setOrderId(result.getString("orderId"));
                payment.setAmount(result.getInt("amount"));
                payment.setCurrentState(State.valueOf(result.getString("currentState")));
                return payment;
            }

            return null;
    }
    }

    // to see the details of an order
    public PaymentDetail findByOrderId(String orderId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE orderId = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, orderId);
        ResultSet result = statement.executeQuery();

        if (result.next()) {
            PaymentDetail payment = new PaymentDetail();
            payment.setPaymentRef(result.getString("paymentRef"));
            payment.setCustomerId(result.getString("customerId"));
            payment.setAmount(result.getInt("amount"));
            payment.setCurrentState(State.valueOf(result.getString("currentState")));
            payment.setCreatedAt(LocalDateTime.parse(result.getString("createdAt")));

            return payment;
        }
        return null;
    }
    }

    // update the currentstate column to approved after calling bank api
    public void updateAuthorization(String paymentRef, String status) throws SQLException {
        String sql = "UPDATE payments SET authorizationId = ? WHERE paymentRef = ?";
        if (status == "approved") {
            String currentState = "APPROVED";

            try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, currentState);
                statement.setString(2, paymentRef);

                statement.executeUpdate();
            }
        }
    }
    
    /*public void updateCapture(String paymentRef, String captureId, String capturedAt) throws SQLException {
        String sql = "UPDATE payments SET captureId = ?, capturedAt = ? WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, captureId);
                statement.setString(2, capturedAt);
                statement.setString(3, paymentRef);

                statement.executeUpdate();
            }
    }

    public void updateVoid(String paymentRef, String voidId, String voidedAt) throws SQLException {
        String sql = "UPDATE payments SET voidId = ?, voidedAt = ? WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, voidId);
                statement.setString(3, paymentRef);
                statement.setString(2, voidedAt);

                statement.executeUpdate();
            }
    }

    public void updateRefund(String paymentRef, String refundId, String refundedAt) throws SQLException {
        String sql = "UPDATE payments SET refundId = ?, refundedAt = ? WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, refundId);
                statement.setString(3, paymentRef);
                statement.setString(2, refundedAt);

                statement.executeUpdate();
            }
    }*/


    // updates the status of the payment depending on the stage the payment is 
    public void updateStatus(String paymentRef, State currentState) throws SQLException {
        String sql = "UPDATE payments SET currentState = ? WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, currentState.toString());
                statement.setString(2, paymentRef);

                statement.executeUpdate();
            }
    }
}
