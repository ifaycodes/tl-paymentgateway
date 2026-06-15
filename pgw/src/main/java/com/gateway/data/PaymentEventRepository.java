package com.gateway.data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import com.gateway.models.PaymentEvent;
import com.gateway.state.State;

@Repository
public class PaymentEventRepository {

    private final DataSource dataSource;

    public PaymentEventRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // to add an order payment to the database
    public void save(PaymentEvent paymentEvent) throws SQLException {
        String sql = """
                INSERT INTO paymentevent (paymentRef, idempotencyKey, bankTransactionId, currentState, timeCreated, notes)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setString(1, paymentEvent.getPaymentRef());
                    statement.setString(2, paymentEvent.getIdempotencyKey());
                    statement.setString(3, paymentEvent.getBankTransactionId());
                    statement.setString(4, paymentEvent.getCurrentState().toString());
                    statement.setTimestamp(5, Timestamp.valueOf(paymentEvent.getTimeCreated()));
                    statement.setString(6, paymentEvent.getNotes());

                    statement.executeUpdate();
                }

    }

    // to find an order in the database using the paymentRef
    public List<PaymentEvent> findByPaymentRef(String paymentRef) throws SQLException {
        String sql = "SELECT * FROM paymentevent WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, paymentRef);

        ResultSet result = statement.executeQuery();

        if (result.next()) {
                List<PaymentEvent> entries = new ArrayList<>();
                PaymentEvent paymentEvent = new PaymentEvent();
                paymentEvent.setPaymentRef(result.getString("paymentRef"));
                paymentEvent.setIdempotencyKey(result.getString("idempotencyKey"));
                paymentEvent.setBankTransactionId(result.getString("bankTransactionId"));
                paymentEvent.setCurrentState(State.valueOf(result.getString("currentState")));
                paymentEvent.setTimeCreated(result.getString("createdAt"));
                entries.add(paymentEvent);
                return entries;
            }

            return null;
    }
    }

    public Boolean findByIdemKey(String idempKey) throws SQLException {
        String sql = "SELECT * FROM paymentevent WHERE idempotencyKey = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, idempKey);

        ResultSet result = statement.executeQuery();

        if (result.next()) {
                return true;
            }

            return false;
        }
    }

    /*public void updateNotes(String note, String paymentRef) throws SQLException {
        String sql = "UPDATE paymentevent SET notes = ? WHERE paymentRef = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, note);
                statement.setString(2, paymentRef);

                statement.executeUpdate();
            }
    }*/

}
