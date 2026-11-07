package com.scc.dao;

import com.scc.model.PrintingService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrintingServiceDao {
    private Connection connection;
    private ActivityLogDao activityLogDao;

    public PrintingServiceDao() {
    }

    public PrintingServiceDao(Connection connection, ActivityLogDao activityLogDao) {
        this.connection = connection;
        this.activityLogDao = activityLogDao;
    }

    public List<PrintingService> getAllServices() {
        List<PrintingService> services = new ArrayList<>();
        String sql = "SELECT id, service_name, price, service_status FROM printing_services";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                services.add(new PrintingService(
                        rs.getInt("id"),
                        rs.getString("service_name"),
                        rs.getDouble("price"),
                        rs.getString("service_status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return services;
    }

    public boolean addService(int adminId, String serviceName, double price) {
        String sql = "INSERT INTO printing_services (service_name, price) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, serviceName);
            stmt.setDouble(2, price);
            if (stmt.executeUpdate() > 0) {
                if (activityLogDao != null) {
                    activityLogDao.logAction(adminId, "SERVICE_CREATED",
                            "Admin " + adminId + " added service: " + serviceName);
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateService(int adminId, int serviceId, String serviceName, double price, String status) {
        String sql = "UPDATE printing_services SET service_name = ?, price = ?, service_status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, serviceName);
            stmt.setDouble(2, price);
            stmt.setString(3, status);
            stmt.setInt(4, serviceId);
            if (stmt.executeUpdate() > 0) {
                if (activityLogDao != null) {
                    activityLogDao.logAction(adminId, "SERVICE_UPDATED",
                            "Admin " + adminId + " updated Service ID " + serviceId
                                    + " (" + status + ")");
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}