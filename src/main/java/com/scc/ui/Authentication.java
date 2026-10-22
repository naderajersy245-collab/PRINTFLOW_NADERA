package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
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

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (password.isBlank()) {
            System.out.println("Password cannot be empty.");
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
            System.out.println("Registration successful!");
            System.out.println("Role: " + role);
            System.out.println("Account status: PENDING");
            System.out.println("You can now log in using your account.");
        }
    }

    private void login() {

        System.out.println("\n--- LOGIN ---");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        User user = userDao.loginUser(username, password);

        if (user == null) {
            System.out.println("Invalid username or password.");
            return;
        }

        System.out.println("\nLogin successful!");
        System.out.println("Welcome, " + user.username());
        System.out.println("Role: " + user.roles());
        System.out.println("Account status: " + user.status());

        routeToRoleMenu(user);
    }

    private void routeToRoleMenu(User user) {

        switch (user.roles()) {
            case "ADMIN" -> new AdminMenu(user, userDao, scanner).start();
            case "CASHIER" -> new CashierMenu(user, userDao, scanner).start();
            case "PRINTING STAFF" -> new PrintingStaffMenu(user, userDao, scanner).start();
            default -> System.out.println("Unknown role: " + user.roles());
        }
    }
}