package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public boolean registerUser(
            String username,
            String password,
            String roles) {

        String sql = """
                INSERT INTO users(username, password, roles, status)VALUES (?, ?, ?, 'PENDING')""";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, roles);
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
     * Changes a user's status from PENDING to APPROVED.
     * Returns true if a row was updated.
     */
    public boolean approveUser(int userId) {

        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Approval error: " + e.getMessage());
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