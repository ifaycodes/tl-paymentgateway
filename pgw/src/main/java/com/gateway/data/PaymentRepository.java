package com.gateway.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.gateway.models.OrderDetail;
import com.gateway.state.State;

@Repository
public class PaymentRepository {

    private final DataSource dataSource;

    public PaymentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // to add an order to the database
    public void save(OrderDetail orderDetail) throws SQLException {
        String sql = """
                INSERT INTO payments (paymentRef, authorizationId, orderId, customerId, amount, currentState, createdAt)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setString(1, orderDetail.getPaymentRef());
                    statement.setString(2, orderDetail.getAuthorizationId());
                    statement.setString(3, orderDetail.getOrderId());
                    statement.setString(4, orderDetail.getCustomerId());
                    statement.setInt(5, orderDetail.getAmount());
                    statement.setString(6, orderDetail.getCurrentState().toString());
                    statement.setTimestamp(7, Timestamp.valueOf(orderDetail.getCreatedAt()));

                    statement.executeUpdate();
                }

    }


    // to find an order in the database using the paymentRef
    public OrderDetail findByPaymentRef(String paymentRef) throws SQLException {
        String sql = "SELECT * FROM payments paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, paymentRef);
        ResultSet result = statement.executeQuery();

        if (result.next()) {
                OrderDetail order = new OrderDetail();
                order.setPaymentRef(result.getString("paymentRef"));
                order.setOrderId(result.getString("orderId"));
                order.setCustomerId(result.getString("customerId"));
                order.setAmount(result.getInt("amount"));
                order.setCurrentState(State.valueOf(result.getString("currentState")));
                order.setAuthorizationId(result.getString("authorizationId"));
                order.setCaptureId(result.getString("captureId"));
                order.setRefundId(result.getString("refundId"));
                order.setVoidId(result.getString("voidId"));
                order.setCreatedAt(LocalDateTime.parse(result.getString("createdAt")));
                order.setVoidedAt(LocalDateTime.parse(result.getString("voidedAt")));
                order.setCapturedAt(LocalDateTime.parse(result.getString("capturedAt")));
                order.setRefundedAt(LocalDateTime.parse(result.getString("refundedAt")));
                return order;
            }

            return null;
    }
    }

    public OrderDetail findByCustomerId(String customerId) throws SQLException {
        String sql = "SELECT * FROM payments customerId = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, customerId);

        ResultSet result = statement.executeQuery();

        if (result.next()) {
                OrderDetail order = new OrderDetail();
                order.setPaymentRef(result.getString("paymentRef"));
                order.setOrderId(result.getString("orderId"));
                order.setAmount(result.getInt("amount"));
                order.setCurrentState(State.valueOf(result.getString("currentState")));
                return order;
            }

            return null;
    }
    }

    public State findByOrderId(String orderId) throws SQLException {
        String sql = "SELECT * FROM payments orderId = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, orderId);
        ResultSet result = statement.executeQuery();

        OrderDetail order = new OrderDetail();
        order.setCurrentState(State.valueOf(result.getString("currentState")));

        State orderStatus = order.getCurrentState();
        return orderStatus;
        }
    }
    
    public void updateCapture(String paymentRef, String captureId, LocalDateTime capturedAt) throws SQLException {
        String sql = "UPDATE payments SET captureId = ?, capturedAt = ? WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, captureId);
                statement.setTimestamp(2, Timestamp.valueOf(capturedAt));
                statement.setString(3, paymentRef);

                statement.executeUpdate();
            }
    }

    public void updateVoid(String paymentRef, String voidId, LocalDateTime voidedAt) throws SQLException {
        String sql = "UPDATE payments SET voidId = ?, voidedAt = ? WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, voidId);
                statement.setString(3, paymentRef);
                statement.setTimestamp(2, Timestamp.valueOf(voidedAt));

                statement.executeUpdate();
            }
    }

    public void updateRefund(String paymentRef, String refundId, LocalDateTime refundedAt) throws SQLException {
        String sql = "UPDATE payments SET refundId = ?, refundedAt = ? WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, refundId);
                statement.setString(3, paymentRef);
                statement.setTimestamp(2, Timestamp.valueOf(refundedAt));

                statement.executeUpdate();
            }
    }


    // updates the status of the order depending on the stage the order is 
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
