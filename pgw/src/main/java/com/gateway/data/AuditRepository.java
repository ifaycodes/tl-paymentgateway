package com.gateway.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.gateway.models.ReceiptDetails;

@Repository
public class AuditRepository {

    private final DataSource dataSource;

    public AuditRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void logResponse(String paymentRef, String response) throws SQLException {
        String sql = """
                INSERT INTO logs (paymentRef, response, timeCreated)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setString(1, paymentRef);
                    statement.setObject(2, response);
                    statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

                    statement.executeUpdate();
                }

    }

    public List<String> findLogsByPaymentRef(String paymentRef) throws SQLException {
        String sql = """
                SELECT * FROM logs ORDER BY timeCreated DESC;
                """;

        List<String> log = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)){
            ResultSet result = statement.executeQuery();

        if (result.next()) {
            log.add(result.getString("paymentRef") + " - " + result.getString("response")); 
        }
        return log;
    }
        
    }

    public ReceiptDetails getReceiptDetail(int receiptId) throws SQLException {
        String sql = """
                SELECT * FROM receipts WHERE receiptId = ?
                """;

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, receiptId);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                ReceiptDetails receiptDetails = new ReceiptDetails();
                receiptDetails.setReceiptId(result.getInt("receiptId"));
                receiptDetails.setPaymentRef(result.getString("paymentRef"));
                receiptDetails.setDateCreated(result.getTimestamp("dateCreated").toLocalDateTime());

                return receiptDetails;
            }

            return null;
        }
    }

    public Boolean getReceiptByRef(String paymentRef) throws SQLException {
        String sql = """
                SELECT * FROM receipts WHERE paymentRef = ?
                """;

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, paymentRef);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return true;
            }

            return false;
        }
    }
}
