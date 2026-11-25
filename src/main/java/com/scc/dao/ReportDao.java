package com.scc.dao;

import com.scc.model.ServiceSales;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportDao {
    private final ActivityLogDao activityLogDao;

    public ReportDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    // yearMonth format: "YYYY-MM". Sorted so the most requested service is first.
    // Cancelled orders are excluded.
    public List<ServiceSales> getMonthlySales(String yearMonth) {
        List<ServiceSales> rows = new ArrayList<>();
        String sql = """
                SELECT ps.service_name,
                       COUNT(o.id)         AS order_count,
                       SUM(o.quantity)     AS total_qty,
                       SUM(o.total_amount) AS total_sales
                FROM orders o
                JOIN printing_services ps ON ps.id = o.service_id
                WHERE strftime('%Y-%m', o.order_date) = ?
                  AND o.order_status <> 'CANCELLED'
                GROUP BY ps.id, ps.service_name
                ORDER BY total_qty DESC, total_sales DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, yearMonth);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ServiceSales(
                            rs.getString("service_name"),
                            rs.getInt("order_count"),
                            rs.getInt("total_qty"),
                            rs.getDouble("total_sales")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error generating report: " + e.getMessage());
        }
        return rows;
    }

    public boolean exportMonthlySalesCsv(int adminId, String yearMonth, List<ServiceSales> rows, Path file) {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.println("Month,Service Name,Orders,Total Quantity,Total Sales");
            int orders = 0;
            int qty = 0;
            double total = 0;
            for (ServiceSales r : rows) {
                out.print(String.format(Locale.US, "%s,%s,%d,%d,%.2f%n", yearMonth, csv(r.serviceName()),
                        r.orderCount(), r.totalQuantity(), r.totalSales()));
                orders += r.orderCount();
                qty += r.totalQuantity();
                total += r.totalSales();
            }
            out.print(String.format(Locale.US, "%s,%s,%d,%d,%.2f%n", yearMonth, "TOTAL", orders, qty, total));
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
            return false;
        }
        activityLogDao.logAction(adminId, "REPORT_EXPORTED",
                "Admin " + adminId + " exported monthly sales report for " + yearMonth);
        return true;
    }

    private String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}