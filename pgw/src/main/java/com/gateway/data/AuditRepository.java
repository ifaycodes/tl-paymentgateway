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
                SELECT * FROM logs WHERE paymentRef = ? ORDER BY timeCreated DESC
                """;

        List<String> log = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1, paymentRef);

            ResultSet result = statement.executeQuery();

        while (result.next()) {
            log.add(result.getString("paymentRef") + " - " + result.getString("response"));
        }
        return log;
    }

    }
}
