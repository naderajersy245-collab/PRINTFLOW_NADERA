package com.scc.ui;

import com.scc.dao.OrderDao;
import com.scc.model.Order;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class PrintingStaffMenu {
    private final User user;
    private final OrderDao orderDao;
    private final Scanner scanner;

    public PrintingStaffMenu(User user, OrderDao orderDao, Scanner scanner) {
        this.user = user;
        this.orderDao = orderDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean active = true;
        while (active) {
            System.out.println("\n========================================");
            System.out.println("      PRINTFLOW - PRINTING STAFF MENU");
            System.out.println("========================================");
            System.out.println("1. Update Order Status (Printing / Ready / Completed)");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> updateOrderStatusFlow();
                case "0" -> {
                    active = false;
                    System.out.println("Logout successful.");
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void updateOrderStatusFlow() {
        System.out.println("\n--- PENDING & ACTIVE ORDERS ---");
        List<Order> orders = orderDao.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No orders found in the database.");
            return;
        }

        String border = "+" + "-".repeat(6) + "+" + "-".repeat(13) + "+" + "-".repeat(22) + "+" + "-".repeat(10) + "+" + "-".repeat(14) + "+";
        String format = "| %-4s | %-11s | %-20s | %-8s | %-12s |%n";

        System.out.println(border);
        System.out.printf(format, "ID", "CUSTOMER", "SERVICE", "QTY", "STATUS");
        System.out.println(border);
        for (Order o : orders) {
            System.out.printf(format, o.id(), o.customerId(), o.serviceName(), o.quantity(), o.status());
        }
        System.out.println(border);

        System.out.print("Enter Order ID to update (0 to cancel): ");
        int orderId;
        try {
            orderId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input!");
            return;
        }

        if (orderId == 0) return;

        System.out.println("\nSelect New Order Status:");
        System.out.println("1. PRINTING");
        System.out.println("2. READY");
        System.out.println("3. COMPLETED");
        System.out.print("Choose status option (1-3): ");
        String statusChoice = scanner.nextLine().trim();

        String newStatus = switch (statusChoice) {
            case "1" -> "PRINTING";
            case "2" -> "READY";
            case "3" -> "COMPLETED";
            default -> null;
        };

        if (newStatus == null) {
            System.out.println("Invalid status choice! Operation cancelled.");
            return;
        }

        if (orderDao.updateOrderStatus(user.id(), orderId, newStatus)) {
            System.out.println("Order status updated to '" + newStatus + "' successfully!");
        } else {
            System.out.println("Failed to update order status. Make sure the Order ID exists.");
        }
    }
}