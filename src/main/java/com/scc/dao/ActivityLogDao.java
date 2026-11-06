package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    /**
     * Inserts a new log entry. The timestamp is filled in by the database.
     * Returns true if the entry was saved.
     */
    public boolean logAction(int userId, String actionType, String description) {

        if (userId <= 0) {
            System.out.println("Logging error: invalid user ID.");
            return false;
        }

        if (actionType == null || actionType.isBlank()) {
            System.out.println("Logging error: action type cannot be empty.");
            return false;
        }

        if (description == null || description.isBlank()) {
            System.out.println("Logging error: description cannot be empty.");
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

    /**
     * Returns all logs, newest first.
     */
    public List<ActivityLog> getAllLogs() {

        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT id, user_id, action_type, description, timestamp "
                + "FROM activity_logs ORDER BY timestamp DESC, id DESC";

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

    /**
     * Returns the logs of one user, newest first.
     * Returns an empty list for an invalid user ID.
     */
    public List<ActivityLog> getLogsByUserId(int userId) {

        List<ActivityLog> logs = new ArrayList<>();

        if (userId <= 0) {
            return logs;
        }

        String sql = "SELECT id, user_id, action_type, description, timestamp "
                + "FROM activity_logs WHERE user_id = ? ORDER BY timestamp DESC, id DESC";

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

    /**
     * Deletes every log entry.
     * Returns the number of deleted rows, or -1 if the operation failed.
     */
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