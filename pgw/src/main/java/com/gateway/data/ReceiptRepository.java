package com.gateway.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;


import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.gateway.models.ReceiptDetails;

@Repository
public class ReceiptRepository {

    private final DataSource dataSource;

    public ReceiptRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(int receiptId, String paymentRef, LocalDateTime dateCreated) throws SQLException {
        String sql = """
                INSERT INTO receipts (receiptId, paymentRef, dateCreated)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setInt(1, receiptId);
                    statement.setString(2, paymentRef);
                    statement.setTimestamp(3, Timestamp.valueOf(dateCreated));

                    statement.executeUpdate();
                }

    }

    public int findLastReceiptId() throws SQLException {
        String sql = """
                SELECT * FROM receipts ORDER BY receiptId DESC LIMIT 1;
                """;

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)){
            ResultSet result = statement.executeQuery();

        if (result.next()) {
            int receiptId = result.getInt("receiptId");
            return receiptId;
        }
        return 0;
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
