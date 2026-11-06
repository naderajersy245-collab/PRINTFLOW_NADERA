package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private static final String ROW_FORMAT = "| %-5s | %-15s | %-16s | %-10s |%n";
    private static final String BORDER = "+" + "-".repeat(7)
            + "+" + "-".repeat(17)
            + "+" + "-".repeat(18)
            + "+" + "-".repeat(12) + "+";

    // Audit log table: %-5s | %-8s | %-18s | %-35s | %-20s
    private static final int DESCRIPTION_WIDTH = 35;
    private static final String LOG_ROW_FORMAT = "| %-5s | %-8s | %-18s | %-35s | %-20s |%n";
    private static final String LOG_BORDER = "+" + "-".repeat(7)
            + "+" + "-".repeat(10)
            + "+" + "-".repeat(20)
            + "+" + "-".repeat(37)
            + "+" + "-".repeat(22) + "+";

    private static final List<String> VALID_ROLES = List.of("ADMIN", "CASHIER", "PRINTING STAFF");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

    private final User user;
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public AdminMenu(User user, UserDao userDao, ActivityLogDao activityLogDao, Scanner scanner) {
        this.user = user;
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.scanner = scanner;
    }

    public void start() {

        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n--- ADMIN MENU ---");
            System.out.println("1. View Account Information");
            System.out.println("2. View All System Users");
            System.out.println("3. View Pending Registrations");
            System.out.println("4. Approve User Account");
            System.out.println("5. Update User Role and Status");
            System.out.println("6. View System Audit Logs");
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewAccountInformation();
                case "2" -> viewAllUsers();
                case "3" -> viewPendingUsers();
                case "4" -> approveUserAccount();
                case "5" -> updateUserRoleAndStatus();
                case "6" -> viewAuditLogs();
                case "7" -> {
                    loggedIn = false;
                    System.out.println("Logout successful.");
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void viewAccountInformation() {
        System.out.println("\n--- ACCOUNT INFORMATION ---");
        System.out.println("ID: " + user.id());
        System.out.println("Username: " + user.username());
        System.out.println("Role: " + user.roles());
        System.out.println("Status: " + user.status());
    }

    private void viewAllUsers() {
        System.out.println("\n--- ALL SYSTEM USERS ---");

        List<User> users = userDao.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        printUserTable(users);
    }

    private void viewPendingUsers() {
        System.out.println("\n--- PENDING REGISTRATIONS ---");

        List<User> pendingUsers = userDao.getPendingUsers();

        if (pendingUsers.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }

        printUserTable(pendingUsers);
    }

    private void approveUserAccount() {
        System.out.println("\n--- APPROVE USER ACCOUNT ---");

        List<User> pendingUsers = userDao.getPendingUsers();

        if (pendingUsers.isEmpty()) {
            System.out.println("No pending accounts to approve.");
            return;
        }

        printUserTable(pendingUsers);

        while (true) {
            int targetId = readIntInput("Enter User ID to approve (0 to cancel): ");

            if (targetId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            if (targetId < 0) {
                System.out.println("User ID must be a positive number.");
                continue;
            }

            boolean isPending = pendingUsers.stream()
                    .anyMatch(pending -> pending.id() == targetId);

            if (!isPending) {
                System.out.println("User ID " + targetId + " is not in the pending list. Try again.");
                continue;
            }

            if (userDao.approveUser(user.id(), targetId)) {
                System.out.println("Success! User ID " + targetId + " has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    /**
     * Lets the admin change the role and status of another user.
     * Entering 0 at any prompt cancels the update.
     */
    private void updateUserRoleAndStatus() {
        System.out.println("\n--- UPDATE USER ROLE AND STATUS ---");

        List<User> users = userDao.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        printUserTable(users);

        // Step 1: target user ID
        int targetUserId;
        while (true) {
            targetUserId = readIntInput("Enter User ID to update (0 to cancel): ");

            if (targetUserId == 0) {
                System.out.println("Update cancelled.");
                return;
            }

            if (targetUserId < 0) {
                System.out.println("User ID must be a positive number.");
                continue;
            }

            // Self-edit restriction
            if (targetUserId == user.id()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }

            final int idToFind = targetUserId;
            boolean exists = users.stream().anyMatch(u -> u.id() == idToFind);

            if (!exists) {
                System.out.println("User ID " + targetUserId + " does not exist. Try again.");
                continue;
            }

            break;
        }

        // Step 2: new role
        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // Step 3: new status
        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // Step 4: save
        if (userDao.updateUserRoleAndStatus(user.id(), targetUserId, newRole, newStatus)) {
            System.out.println("Success! User ID " + targetUserId
                    + " has been updated to role " + newRole + " and status " + newStatus + ".");
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    // ------------------------------------------------------------------
    // AUDIT LOGS
    // ------------------------------------------------------------------

    /**
     * Audit log sub-menu: view all, filter by user ID, or clear logs.
     * Invalid choices re-prompt and never crash the program.
     */
    private void viewAuditLogs() {

        boolean back = false;

        while (!back) {
            System.out.println("\n--- SYSTEM AUDIT LOGS ---");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear All Logs");
            System.out.println("0. Back to Admin Menu");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllLogs();
                case "2" -> filterLogsByUserId();
                case "3" -> clearAllLogs();
                case "0" -> back = true;
                default -> System.out.println("Invalid choice. Please enter 1, 2, 3, or 0.");
            }
        }
    }

    private void viewAllLogs() {
        System.out.println("\n--- ALL SYSTEM LOGS ---");

        List<ActivityLog> logs = activityLogDao.getAllLogs();

        if (logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }

        printLogTable(logs);
    }

    /**
     * Keeps asking until the admin enters an existing user ID.
     * Non-numeric input, negative numbers, and unknown IDs re-prompt.
     * Entering 0 cancels.
     */
    private void filterLogsByUserId() {
        System.out.println("\n--- FILTER LOGS BY USER ID ---");

        List<User> users = userDao.getAllUsers();

        int targetUserId;
        while (true) {
            targetUserId = readIntInput("Enter User ID to filter (0 to cancel): ");

            if (targetUserId == 0) {
                System.out.println("Filter cancelled.");
                return;
            }

            if (targetUserId < 0) {
                System.out.println("User ID must be a positive number.");
                continue;
            }

            final int idToFind = targetUserId;
            boolean exists = users.stream().anyMatch(u -> u.id() == idToFind);

            if (!exists) {
                System.out.println("User ID " + targetUserId + " does not exist. Try again.");
                continue;
            }

            break;
        }

        List<ActivityLog> logs = activityLogDao.getLogsByUserId(targetUserId);

        if (logs.isEmpty()) {
            System.out.println("No activity logs found for User ID " + targetUserId + ".");
            return;
        }

        printLogTable(logs);
    }

    /**
     * Deletes all logs only after the admin types CLEAR exactly.
     * The clearing itself is recorded as a new LOGS_CLEARED entry.
     */
    private void clearAllLogs() {
        System.out.println("\n--- CLEAR ALL LOGS ---");

        int total = activityLogDao.getAllLogs().size();

        if (total == 0) {
            System.out.println("There are no logs to clear.");
            return;
        }

        System.out.println("WARNING: This will permanently delete " + total + " log entries.");
        System.out.print("Type CLEAR to confirm (anything else cancels): ");
        String confirmation = scanner.nextLine().trim();

        if (!confirmation.equals("CLEAR")) {
            System.out.println("Clear cancelled. No logs were deleted.");
            return;
        }

        int deleted = activityLogDao.clearAllLogs();

        if (deleted < 0) {
            System.out.println("Failed to clear logs. Please try again.");
            return;
        }

        activityLogDao.logAction(
                user.id(),
                "LOGS_CLEARED",
                "Admin " + user.id() + " cleared " + deleted + " log entries");

        System.out.println("Success! " + deleted + " log entries were deleted.");
    }

    /**
     * Prints logs as an aligned table. Long descriptions are wrapped onto
     * extra lines so the columns stay aligned and no text is cut off.
     */
    private void printLogTable(List<ActivityLog> logs) {
        System.out.println(LOG_BORDER);
        System.out.printf(LOG_ROW_FORMAT, "ID", "USER ID", "ACTION TYPE", "DESCRIPTION", "TIMESTAMP");
        System.out.println(LOG_BORDER);

        for (ActivityLog log : logs) {
            List<String> lines = wrapText(log.description(), DESCRIPTION_WIDTH);

            System.out.printf(LOG_ROW_FORMAT,
                    log.id(), log.userId(), log.actionType(), lines.get(0), log.timestamp());

            for (int i = 1; i < lines.size(); i++) {
                System.out.printf(LOG_ROW_FORMAT, "", "", "", lines.get(i), "");
            }
        }

        System.out.println(LOG_BORDER);
        System.out.println("Total entries: " + logs.size());
    }

    private List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();

        while (remaining.length() > width) {
            int breakAt = remaining.lastIndexOf(' ', width);
            if (breakAt <= 0) {
                breakAt = width;
            }
            lines.add(remaining.substring(0, breakAt).trim());
            remaining = remaining.substring(breakAt).trim();
        }

        lines.add(remaining);
        return lines;
    }

    // ------------------------------------------------------------------
    // INPUT HELPERS
    // ------------------------------------------------------------------

    /**
     * Keeps asking until the admin enters a whole number.
     * Letters or symbols never crash the program.
     */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    /**
     * Keeps asking until a valid role is entered.
     * Returns null if the admin enters 0 to cancel.
     */
    private String readValidRole() {
        while (true) {
            System.out.println("\nValid roles: " + String.join(", ", VALID_ROLES));
            System.out.print("Enter new role (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.equals("0")) {
                return null;
            }

            if (VALID_ROLES.contains(input)) {
                return input;
            }

            System.out.println("Invalid role. Please choose from: " + String.join(", ", VALID_ROLES));
        }
    }

    /**
     * Keeps asking until a valid status is entered.
     * Returns null if the admin enters 0 to cancel.
     */
    private String readValidStatus() {
        while (true) {
            System.out.println("\nValid statuses: " + String.join(", ", VALID_STATUSES));
            System.out.print("Enter new status (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.equals("0")) {
                return null;
            }

            if (VALID_STATUSES.contains(input)) {
                return input;
            }

            System.out.println("Invalid status. Please choose from: " + String.join(", ", VALID_STATUSES));
        }
    }

    /**
     * Prints any list of users as a formatted, aligned table.
     */
    private void printUserTable(List<User> users) {
        System.out.println(BORDER);
        System.out.printf(ROW_FORMAT, "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(BORDER);

        for (User u : users) {
            System.out.printf(ROW_FORMAT, u.id(), u.username(), u.roles(), u.status());
        }

        System.out.println(BORDER);
    }
}