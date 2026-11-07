package com.scc.dao;

import com.scc.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {
    private Connection connection;
    private ActivityLogDao activityLogDao;

    public OrderDao() {
    }

    public OrderDao(Connection connection, ActivityLogDao activityLogDao) {
        this.connection = connection;
        this.activityLogDao = activityLogDao;
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, customer_id, service_id, quantity, total_amount, order_status, order_date FROM orders";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                orders.add(new Order(
                        rs.getInt("id"),
                        rs.getInt("customer_id"),
                        "Service ID: " + rs.getInt("service_id"),
                        rs.getInt("quantity"),
                        rs.getDouble("total_amount"),
                        rs.getString("order_status"),
                        rs.getString("order_date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    // Core Business Process log (checklist event #3)
    public boolean createOrder(int cashierId, int customerId, int serviceId, int quantity, double totalAmount) {
        String sql = "INSERT INTO orders (customer_id, service_id, quantity, total_amount, order_status, payment_status, amount_paid) VALUES (?, ?, ?, ?, 'PENDING', 'PAID', ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setInt(2, serviceId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, totalAmount);
            stmt.setDouble(5, totalAmount);
            if (stmt.executeUpdate() > 0) {
                if (activityLogDao != null) {
                    activityLogDao.logAction(cashierId, "ORDER_CREATED",
                            "Processed order for Customer " + customerId
                                    + " - Total: P" + String.format("%.2f", totalAmount));
                    activityLogDao.logAction(cashierId, "PAYMENT_RECORDED",
                            "Recorded payment of P" + String.format("%.2f", totalAmount)
                                    + " for Customer " + customerId);
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Lifecycle / State Change log (checklist event #4)
    public boolean updateOrderStatus(int staffId, int orderId, String newStatus) {
        String oldStatus = null;
        try (PreparedStatement sel = connection.prepareStatement("SELECT order_status FROM orders WHERE id = ?")) {
            sel.setInt(1, orderId);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    oldStatus = rs.getString("order_status");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        if (oldStatus == null) {
            return false; // order ID doesn't exist
        }

        String sql = "UPDATE orders SET order_status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            if (stmt.executeUpdate() > 0) {
                if (activityLogDao != null) {
                    activityLogDao.logAction(staffId, "ORDER_UPDATED",
                            "Updated Order #" + orderId + " status from " + oldStatus + " to " + newStatus);
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}