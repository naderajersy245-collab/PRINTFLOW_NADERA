package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 30;

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, ActivityLogDao activityLogDao, Scanner scanner) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.scanner = scanner;
    }

    public void start() {

        boolean running = true;

        while (running) {
            System.out.println("\n================================");
            System.out.println("        PRINTFLOW SYSTEM");
            System.out.println("================================");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    running = false;
                    System.out.println("Application closed. Goodbye!");
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void register() {

        System.out.println("\n--- REGISTER ACCOUNT ---");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }

        if (!isValidUsername(username)) {
            System.out.println("Username must be " + MIN_USERNAME_LENGTH + " to " + MAX_USERNAME_LENGTH
                    + " characters and contain only letters, numbers, or underscores.");
            return;
        }

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (password.isBlank()) {
            System.out.println("Password cannot be empty.");
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            System.out.println("Password must be " + MIN_PASSWORD_LENGTH + " to "
                    + MAX_PASSWORD_LENGTH + " characters long.");
            return;
        }

        System.out.println("\nSelect your role:");
        System.out.println("1. ADMIN");
        System.out.println("2. CASHIER");
        System.out.println("3. PRINTING STAFF");
        System.out.print("Choose a role: ");

        String roleChoice = scanner.nextLine().trim();
        String role;

        switch (roleChoice) {
            case "1" -> role = "ADMIN";
            case "2" -> role = "CASHIER";
            case "3" -> role = "PRINTING STAFF";
            default -> {
                System.out.println("Invalid role.");
                return;
            }
        }

        boolean success = userDao.registerUser(username, password, role);

        if (success) {
            boolean isAdmin = role.equals("ADMIN");
            String status = isAdmin ? "APPROVED" : "PENDING";

            System.out.println("Registration successful!");
            System.out.println("Role: " + role);
            System.out.println("Account status: " + status);

            if (isAdmin) {
                System.out.println("You can now log in using your account.");
            } else {
                System.out.println("Your account must be approved by an Admin before you can log in.");
            }
        }
    }

    private void login() {

        System.out.println("\n--- LOGIN ---");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (password.isBlank()) {
            System.out.println("Password cannot be empty.");
            return;
        }

        User user = userDao.loginUser(username, password);

        if (user == null) {
            System.out.println("Invalid username or password.");
            return;
        }

        // Only approved accounts may enter the system
        if (!"APPROVED".equalsIgnoreCase(user.status())) {
            System.out.println("Your account is pending approval. Please wait for an Admin to approve it.");
            return;
        }

        System.out.println("\nLogin successful!");
        System.out.println("Welcome, " + user.username());
        System.out.println("Role: " + user.roles());
        System.out.println("Account status: " + user.status());

        activityLogDao.logAction(
                user.id(),
                "LOGIN",
                "User " + user.username() + " (ID: " + user.id() + ") logged in.");

        routeToRoleMenu(user);
    }

    private void routeToRoleMenu(User user) {

        switch (user.roles()) {
            case "ADMIN" -> new AdminMenu(user, userDao, activityLogDao, scanner).start();
            case "CASHIER" -> new CashierMenu(user, userDao, scanner).start();
            case "PRINTING STAFF" -> new PrintingStaffMenu(user, userDao, scanner).start();
            default -> {
                System.out.println("Unknown role: " + user.roles());
                return;
            }
        }

        // The role menu only returns after the user chose Logout
        logout(user);
    }

    private void logout(User user) {
        activityLogDao.logAction(
                user.id(),
                "LOGOUT",
                "User " + user.username() + " (ID: " + user.id() + ") logged out.");
    }

    private boolean isValidUsername(String username) {
        return username.length() >= MIN_USERNAME_LENGTH
                && username.length() <= MAX_USERNAME_LENGTH
                && username.matches("[A-Za-z0-9_]+");
    }
}