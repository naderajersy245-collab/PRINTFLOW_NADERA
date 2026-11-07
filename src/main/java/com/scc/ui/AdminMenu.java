package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.OrderDao;
import com.scc.dao.PrintingServiceDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.Order;
import com.scc.model.PrintingService;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    // Required audit log table format
    private static final String LOG_ROW_FORMAT = "%-5s | %-8s | %-18s | %-35s | %-20s%n";

    private static final String USER_ROW_FORMAT = "| %-5s | %-15s | %-16s | %-10s |%n";
    private static final String USER_BORDER = "+" + "-".repeat(7)
            + "+" + "-".repeat(17)
            + "+" + "-".repeat(18)
            + "+" + "-".repeat(12) + "+";

    private final User user;
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final PrintingServiceDao printingServiceDao;
    private final OrderDao orderDao;
    private final Scanner scanner;

    public AdminMenu(User user, UserDao userDao, ActivityLogDao activityLogDao,
                     PrintingServiceDao printingServiceDao, OrderDao orderDao, Scanner scanner) {
        this.user = user;
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.printingServiceDao = printingServiceDao;
        this.orderDao = orderDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n========================================");
            System.out.println("          PRINTFLOW - ADMIN MENU");
            System.out.println("========================================");
            System.out.println("1. User Management (CRUD)");
            System.out.println("2. Printing Services Management (CRUD)");
            System.out.println("3. View Printing Shop Orders");
            System.out.println("4. View System Audit Logs");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> manageUsersMenu();
                case "2" -> managePrintingServices();
                case "3" -> viewShopOrders();
                case "4" -> viewAuditLogs();
                case "0" -> {
                    loggedIn = false;
                    System.out.println("Logout successful.");
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ---------------------------------------------------------------
    // USER MANAGEMENT
    // ---------------------------------------------------------------
    private void manageUsersMenu() {
        boolean managing = true;
        while (managing) {
            System.out.println("\n--- USER MANAGEMENT (CRUD) ---");
            System.out.println("1. View All Users (Read)");
            System.out.println("2. View Pending Registrations & Approve");
            System.out.println("3. Update User Role and Status (Update)");
            System.out.println("4. Delete User Account (Delete)");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> printUserTable(userDao.getAllUsers());
                case "2" -> approvePendingUsersFlow();
                case "3" -> updateUserRoleAndStatus();
                case "4" -> deleteUserFlow();
                case "0" -> managing = false;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void approvePendingUsersFlow() {
        List<User> pending = userDao.getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("No pending accounts found.");
            return;
        }
        printUserTable(pending);
        System.out.print("Enter User ID to approve (0 to cancel): ");
        try {
            int id = Integer.parseInt(scanner.nextLine().trim());
            if (id == 0) {
                System.out.println("Cancelled.");
            } else if (pending.stream().anyMatch(u -> u.id() == id) && userDao.approveUser(user.id(), id)) {
                System.out.println("User ID " + id + " approved successfully.");
            } else {
                System.out.println("Approval failed. Make sure the User ID is in the pending list.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a valid number.");
        }
    }

    private void updateUserRoleAndStatus() {
        while (true) {
            printUserTable(userDao.getAllUsers());
            System.out.print("Enter User ID to update (0 to cancel): ");
            String input = scanner.nextLine().trim();

            int targetId;
            try {
                targetId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid number.");
                continue;
            }

            if (targetId == 0) {
                System.out.println("Cancelled. Returning to User Management.");
                break;
            }

            System.out.println("\nSelect New Role:");
            System.out.println("1. ADMIN");
            System.out.println("2. CASHIER");
            System.out.println("3. PRINTING STAFF");
            System.out.print("Choose role option (1-3): ");
            String roleChoice = scanner.nextLine().trim();

            String role = switch (roleChoice) {
                case "1" -> "ADMIN";
                case "2" -> "CASHIER";
                case "3" -> "PRINTING STAFF";
                default -> null;
            };

            if (role == null) {
                System.out.println("Invalid role choice! Please try again.");
                continue;
            }

            System.out.println("\nSelect New Status:");
            System.out.println("1. APPROVED");
            System.out.println("2. PENDING");
            System.out.print("Choose status option (1-2): ");
            String statusChoice = scanner.nextLine().trim();

            String status = switch (statusChoice) {
                case "1" -> "APPROVED";
                case "2" -> "PENDING";
                default -> null;
            };

            if (status == null) {
                System.out.println("Invalid status choice! Please try again.");
                continue;
            }

            if (userDao.updateUserRoleAndStatus(user.id(), targetId, role, status)) {
                System.out.println("User updated successfully.");
            } else {
                System.out.println("Update failed. Make sure the User ID exists.");
            }

            break;
        }
    }

    private void deleteUserFlow() {
        while (true) {
            printUserTable(userDao.getAllUsers());
            System.out.print("Enter User ID to delete (0 to cancel): ");
            String input = scanner.nextLine().trim();

            int targetId;
            try {
                targetId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid number.");
                continue;
            }

            if (targetId == 0) {
                System.out.println("Cancelled. Returning to User Management.");
                break;
            }

            if (targetId == user.id()) {
                System.out.println("Error: You cannot delete your own active admin account!");
                continue;
            }

            System.out.print("Are you sure you want to delete User ID " + targetId + "? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();

            if (confirm.equals("y") || confirm.equals("yes")) {
                if (userDao.deleteUser(user.id(), targetId)) {
                    System.out.println("User ID " + targetId + " deleted successfully.");
                } else {
                    System.out.println("Deletion failed. Make sure the User ID exists.");
                }
            } else {
                System.out.println("Deletion cancelled.");
            }
            break;
        }
    }

    private void printUserTable(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        System.out.println(USER_BORDER);
        System.out.printf(USER_ROW_FORMAT, "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(USER_BORDER);
        for (User u : users) {
            System.out.printf(USER_ROW_FORMAT, u.id(), u.username(), u.roles(), u.status());
        }
        System.out.println(USER_BORDER);
    }

    // ---------------------------------------------------------------
    // PRINTING SERVICES MANAGEMENT
    // ---------------------------------------------------------------
    private void managePrintingServices() {
        boolean managing = true;
        while (managing) {
            System.out.println("\n--- PRINTING SERVICES MANAGEMENT ---");
            System.out.println("1. View All Services (Read)");
            System.out.println("2. Add New Service (Create)");
            System.out.println("3. Update Service Details / Status (Update)");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> printServiceTable(printingServiceDao.getAllServices());
                case "2" -> addServiceFlow();
                case "3" -> updateServiceFlow();
                case "0" -> managing = false;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void addServiceFlow() {
        System.out.print("Enter service name (e.g., Tarpaulin Printing): ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Service name cannot be empty. Operation cancelled.");
            return;
        }

        System.out.print("Enter price: ");
        double price;
        try {
            price = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid price format! Operation cancelled.");
            return;
        }
        if (price < 0) {
            System.out.println("Price cannot be negative. Operation cancelled.");
            return;
        }

        if (printingServiceDao.addService(user.id(), name, price)) {
            System.out.println("Printing service added successfully.");
        } else {
            System.out.println("Failed to add printing service (the name may already exist).");
        }
    }

    private void updateServiceFlow() {
        List<PrintingService> services = printingServiceDao.getAllServices();
        if (services.isEmpty()) {
            System.out.println("No services found.");
            return;
        }
        printServiceTable(services);

        System.out.print("Enter Service ID to update (0 to cancel): ");
        int serviceId;
        try {
            serviceId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input!");
            return;
        }

        if (serviceId == 0) return;

        System.out.print("Enter new service name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Service name cannot be empty. Operation cancelled.");
            return;
        }

        System.out.print("Enter new price: ");
        double price;
        try {
            price = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid price format!");
            return;
        }
        if (price < 0) {
            System.out.println("Price cannot be negative. Operation cancelled.");
            return;
        }

        System.out.print("Enter status (ACTIVE / INACTIVE): ");
        String status = scanner.nextLine().trim().toUpperCase();
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            System.out.println("Invalid status! Use ACTIVE or INACTIVE. Operation cancelled.");
            return;
        }

        if (printingServiceDao.updateService(user.id(), serviceId, name, price, status)) {
            System.out.println("Service updated successfully.");
        } else {
            System.out.println("Update failed. Make sure the Service ID exists and the name is unique.");
        }
    }

    private void printServiceTable(List<PrintingService> services) {
        if (services.isEmpty()) {
            System.out.println("No printing services available.");
            return;
        }
        String border = "+" + "-".repeat(6) + "+" + "-".repeat(25) + "+" + "-".repeat(10) + "+" + "-".repeat(16) + "+";
        String format = "| %-4s | %-23s | %-8s | %-14s |%n";

        System.out.println(border);
        System.out.printf(format, "ID", "SERVICE NAME", "PRICE", "STATUS");
        System.out.println(border);
        for (PrintingService s : services) {
            System.out.printf(format, s.id(), s.serviceName(), s.price(), s.status());
        }
        System.out.println(border);
    }

    // ---------------------------------------------------------------
    // ORDERS
    // ---------------------------------------------------------------
    private void viewShopOrders() {
        System.out.println("\n--- PRINTING SHOP ORDERS ---");
        List<Order> orders = orderDao.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No printing orders found in the database.");
            return;
        }

        String border = "+" + "-".repeat(6) + "+" + "-".repeat(13) + "+" + "-".repeat(22) + "+" + "-".repeat(10) + "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(21) + "+";
        String format = "| %-4s | %-11s | %-20s | %-8s | %-12s | %-12s | %-19s |%n";

        System.out.println(border);
        System.out.printf(format, "ID", "CUSTOMER ID", "SERVICE NAME", "QTY", "TOTAL PRICE", "STATUS", "CREATED AT");
        System.out.println(border);
        for (Order o : orders) {
            System.out.printf(format, o.id(), o.customerId(), o.serviceName(), o.quantity(), o.totalPrice(), o.status(), o.createdAt());
        }
        System.out.println(border);
    }

    // ---------------------------------------------------------------
    // SYSTEM AUDIT LOGS (Deliverable 8)
    // ---------------------------------------------------------------
    private void viewAuditLogs() {
        while (true) {
            System.out.println("\n--- SYSTEM AUDIT LOGS ---");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear All Logs");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> printLogs(activityLogDao.getAllLogs());
                case "2" -> filterLogsByUserId();
                case "3" -> {
                    System.out.print("This permanently deletes ALL logs. Type YES to confirm: ");
                    if (scanner.nextLine().trim().equals("YES")) {
                        System.out.println(activityLogDao.clearAllLogs() + " log(s) cleared.");
                    } else {
                        System.out.println("Cancelled.");
                    }
                }
                case "0" -> {
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void filterLogsByUserId() {
        while (true) {
            System.out.print("Enter User ID to filter (0 to cancel): ");
            String input = scanner.nextLine().trim();
            try {
                int id = Integer.parseInt(input);
                if (id == 0) return;
                printLogs(activityLogDao.getLogsByUserId(id));
                return;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
            }
        }
    }

    private void printLogs(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }
        String line = "-".repeat(5) + "-+-" + "-".repeat(8) + "-+-" + "-".repeat(18)
                + "-+-" + "-".repeat(35) + "-+-" + "-".repeat(20);
        System.out.println();
        System.out.printf(LOG_ROW_FORMAT, "ID", "USER ID", "ACTION TYPE", "DESCRIPTION", "TIMESTAMP");
        System.out.println(line);
        for (ActivityLog l : logs) {
            String desc = l.description();
            String first = desc.length() > 35 ? desc.substring(0, 35) : desc;
            System.out.printf(LOG_ROW_FORMAT, l.id(), l.userId(), l.actionType(), first, l.timestamp());
            // wrap long descriptions onto extra lines so nothing is cut off
            for (int i = 35; i < desc.length(); i += 35) {
                System.out.printf(LOG_ROW_FORMAT, "", "", "",
                        desc.substring(i, Math.min(i + 35, desc.length())), "");
            }
        }
        System.out.println(line);
        System.out.println(logs.size() + " log(s) shown.");
    }
}