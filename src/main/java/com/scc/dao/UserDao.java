package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private static final List<String> VALID_ROLES = List.of("ADMIN", "CASHIER", "PRINTING STAFF");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public boolean registerUser(
            String username,
            String password,
            String roles) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()
                || roles == null || roles.isBlank()) {
            System.out.println("Registration error: all fields are required.");
            return false;
        }

        String role = roles.trim().toUpperCase();

        if (!VALID_ROLES.contains(role)) {
            System.out.println("Registration error: invalid role.");
            return false;
        }

        String sql = "INSERT INTO users(username, password, roles, status) VALUES (?, ?, ?, ?)";

        // Admins are auto-approved because only an admin can approve accounts
        String status = role.equals("ADMIN") ? "APPROVED" : "PENDING";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(2, password);
            statement.setString(3, role);
            statement.setString(4, status);
            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                System.out.println("Username already exists.");
            } else {
                System.out.println("Registration error: " + e.getMessage());
            }

            return false;
        }
    }

    public User loginUser(String username, String password) {

        String sql = "SELECT id, username, password, roles, status FROM users WHERE username = ? AND password = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return mapUser(result);
                }
            }

        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }

        return null;
    }

    /**
     * Returns every registered account regardless of role or status.
     */
    public List<User> getAllUsers() {

        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password, roles, status FROM users ORDER BY id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                users.add(mapUser(result));
            }

        } catch (SQLException e) {
            System.out.println("Error loading users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Returns every account with status PENDING, across all roles.
     */
    public List<User> getPendingUsers() {

        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password, roles, status FROM users WHERE status = ? ORDER BY id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "PENDING");

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    users.add(mapUser(result));
                }
            }

        } catch (SQLException e) {
            System.out.println("Error loading pending users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Changes a user's status from PENDING to APPROVED and records a
     * USER_APPROVED entry in the activity log.
     * Only accounts that are currently PENDING can be approved.
     * Returns true if a row was updated.
     */
    public boolean approveUser(int adminId, int userId) {

        if (adminId <= 0 || userId <= 0) {
            System.out.println("Approval error: invalid user ID.");
            return false;
        }

        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ? AND status = 'PENDING'";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                activityLogDao.logAction(
                        adminId,
                        "USER_APPROVED",
                        "Admin " + adminId + " approved account for User ID " + userId);
            }

            return updated;

        } catch (SQLException e) {
            System.out.println("Approval error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates both the role and the status of a user and records a
     * USER_UPDATED entry in the activity log.
     * Inputs are converted to upper case before being validated and saved.
     * Returns true if a row was updated.
     */
    public boolean updateUserRoleAndStatus(int adminId, int userId, String newRole, String newStatus) {

        if (adminId <= 0 || userId <= 0) {
            System.out.println("Update error: invalid user ID.");
            return false;
        }

        if (adminId == userId) {
            System.out.println("Update error: an admin cannot modify their own account.");
            return false;
        }

        if (newRole == null || newStatus == null) {
            System.out.println("Update error: role and status are required.");
            return false;
        }

        String role = newRole.trim().toUpperCase();
        String status = newStatus.trim().toUpperCase();

        if (!VALID_ROLES.contains(role)) {
            System.out.println("Update error: invalid role.");
            return false;
        }

        if (!VALID_STATUSES.contains(status)) {
            System.out.println("Update error: invalid status.");
            return false;
        }

        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, role);
            statement.setString(2, status);
            statement.setInt(3, userId);
            boolean updated = statement.executeUpdate() > 0;

            if (updated) {
                activityLogDao.logAction(
                        adminId,
                        "USER_UPDATED",
                        "Admin " + adminId + " changed role of User " + userId
                                + " to " + role + " and status to " + status);
            }

            return updated;

        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        return new User(
                result.getInt("id"),
                result.getString("username"),
                result.getString("password"),
                result.getString("roles"),
                result.getString("status")
        );
    }
}