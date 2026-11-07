package com.scc.dao;

import com.scc.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {
    private final Connection connection;
    private final ActivityLogDao activityLogDao;

    public CustomerDao(Connection connection, ActivityLogDao activityLogDao) {
        this.connection = connection;
        this.activityLogDao = activityLogDao;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT id, customer_name, contact_number, address FROM customers";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Customer(
                        rs.getInt("id"),
                        rs.getString("customer_name"),
                        rs.getString("contact_number"),
                        rs.getString("address")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Returns the new customer's ID, or -1 on failure
    public int addCustomer(int userId, String name, String contact, String address) {
        String sql = "INSERT INTO customers (customer_name, contact_number, address) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, contact);
            stmt.setString(3, address);
            if (stmt.executeUpdate() > 0) {
                int newId = -1;
                try (Statement s = connection.createStatement();
                     ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) newId = rs.getInt(1);
                }
                if (activityLogDao != null) {
                    activityLogDao.logAction(userId, "CUSTOMER_CREATED",
                            "Cashier " + userId + " added customer ID " + newId + ": " + name);
                }
                return newId;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}