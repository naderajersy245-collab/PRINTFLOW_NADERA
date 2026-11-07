package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    public boolean logAction(int userId, String actionType, String description) {

        if (userId <= 0 || actionType == null || actionType.isBlank()
                || description == null || description.isBlank()) {
            return false;
        }

        String sql = "INSERT INTO activity_logs(user_id, action_type, description) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, actionType.trim().toUpperCase());
            statement.setString(3, description.trim());
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Logging error: " + e.getMessage());
            return false;
        }
    }

    public List<ActivityLog> getAllLogs() {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT id, user_id, action_type, description, timestamp FROM activity_logs ORDER BY timestamp DESC, id DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                logs.add(mapLog(result));
            }

        } catch (SQLException e) {
            System.out.println("Error loading logs: " + e.getMessage());
        }

        return logs;
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        List<ActivityLog> logs = new ArrayList<>();
        if (userId <= 0) return logs;

        String sql = "SELECT id, user_id, action_type, description, timestamp FROM activity_logs WHERE user_id = ? ORDER BY timestamp DESC, id DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    logs.add(mapLog(result));
                }
            }

        } catch (SQLException e) {
            System.out.println("Error loading logs for user: " + e.getMessage());
        }

        return logs;
    }

    public int clearAllLogs() {
        String sql = "DELETE FROM activity_logs";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error clearing logs: " + e.getMessage());
            return -1;
        }
    }

    private ActivityLog mapLog(ResultSet result) throws SQLException {
        return new ActivityLog(
                result.getInt("id"),
                result.getInt("user_id"),
                result.getString("action_type"),
                result.getString("description"),
                result.getString("timestamp")
        );
    }
}