package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private static final String ROW_FORMAT = "| %-5s | %-15s | %-16s | %-10s |%n";
    private static final String BORDER = "+" + "-".repeat(7)
            + "+" + "-".repeat(17)
            + "+" + "-".repeat(18)
            + "+" + "-".repeat(12) + "+";

    private final User user;
    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(User user, UserDao userDao, Scanner scanner) {
        this.user = user;
        this.userDao = userDao;
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
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewAccountInformation();
                case "2" -> viewAllUsers();
                case "3" -> viewPendingUsers();
                case "4" -> approveUserAccount();
                case "5" -> {
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
            System.out.print("Enter User ID to approve (0 to cancel): ");
            String input = scanner.nextLine().trim();

            int targetId;
            try {
                targetId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            if (targetId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            boolean isPending = pendingUsers.stream()
                    .anyMatch(pending -> pending.id() == targetId);

            if (!isPending) {
                System.out.println("User ID " + targetId + " is not in the pending list. Try again.");
                continue;
            }

            if (userDao.approveUser(targetId)) {
                System.out.println("Success! User ID " + targetId + " has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
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