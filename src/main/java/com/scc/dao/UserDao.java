package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private Connection connection;
    private ActivityLogDao activityLogDao;

    public UserDao() {
    }

    public UserDao(Connection connection, ActivityLogDao activityLogDao) {
        this.connection = connection;
        this.activityLogDao = activityLogDao;
    }

    public boolean registerUser(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, roles, status) VALUES (?, ?, ?, 'PENDING')";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public User loginUser(String username, String password) {
        String sql = "SELECT id, username, password, roles, status FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password, roles, status FROM users";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public List<User> getPendingUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password, roles, status FROM users WHERE status = 'PENDING'";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Account Management log (checklist event #2)
    public boolean approveUser(int adminId, int targetId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, targetId);
            if (stmt.executeUpdate() > 0) {
                if (activityLogDao != null) {
                    activityLogDao.logAction(adminId, "USER_APPROVED",
                            "Admin " + adminId + " approved User " + targetId);
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Account Management log (checklist event #2)
    public boolean updateUserRoleAndStatus(int adminId, int targetId, String role, String status) {
        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, role);
            stmt.setString(2, status);
            stmt.setInt(3, targetId);
            if (stmt.executeUpdate() > 0) {
                if (activityLogDao != null) {
                    activityLogDao.logAction(adminId, "USER_UPDATED",
                            "Admin " + adminId + " changed role of User " + targetId
                                    + " to " + role + " (status: " + status + ")");
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteUser(int adminId, int targetId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, targetId);
            if (stmt.executeUpdate() > 0) {
                if (activityLogDao != null) {
                    activityLogDao.logAction(adminId, "USER_DELETED",
                            "Admin " + adminId + " deleted User " + targetId);
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("roles"),
                rs.getString("status")
        );
    }
}