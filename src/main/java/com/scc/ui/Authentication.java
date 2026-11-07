package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.CustomerDao;
import com.scc.dao.OrderDao;
import com.scc.dao.PrintingServiceDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final PrintingServiceDao printingServiceDao;
    private final CustomerDao customerDao;
    private final OrderDao orderDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, ActivityLogDao activityLogDao,
                          PrintingServiceDao printingServiceDao, CustomerDao customerDao,
                          OrderDao orderDao, Scanner scanner) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.printingServiceDao = printingServiceDao;
        this.customerDao = customerDao;
        this.orderDao = orderDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n========================================");
            System.out.println("          PRINTFLOW - LOGIN SYSTEM");
            System.out.println("========================================");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleRegister();
                case "2" -> handleLogin();
                case "3" -> {
                    running = false;
                    System.out.println("Exiting system. Goodbye!");
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void handleLogin() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        User user = userDao.loginUser(username, password);
        if (user == null) {
            System.out.println("Login failed! Invalid username or password.");
            return;
        }

        if (!"APPROVED".equalsIgnoreCase(user.status())) {
            System.out.println("Login failed! Your account status is PENDING approval by admin.");
            return;
        }

        login(user);
    }

    // Authentication log (checklist event #1): logs login, runs the role menu, then logs logout
    private void login(User user) {
        activityLogDao.logAction(user.id(), "USER_LOGIN",
                "User " + user.username() + " (ID: " + user.id() + ") logged in.");

        System.out.println("Login successful! Welcome, " + user.username() + " (" + user.roles() + ")");

        String role = user.roles().toUpperCase();
        if (role.contains("ADMIN")) {
            new AdminMenu(user, userDao, activityLogDao, printingServiceDao, orderDao, scanner).start();
        } else if (role.contains("CASHIER")) {
            new CashierMenu(user, orderDao, printingServiceDao, customerDao, scanner).start();
        } else if (role.contains("STAFF")) {
            new PrintingStaffMenu(user, orderDao, scanner).start();
        } else {
            System.out.println("Error: Unrecognized user role.");
        }

        logout(user);
    }

    private void logout(User user) {
        activityLogDao.logAction(user.id(), "USER_LOGOUT",
                "User " + user.username() + " (ID: " + user.id() + ") logged out.");
    }

    private void handleRegister() {
        System.out.print("Enter desired username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password cannot be empty. Registration cancelled.");
            return;
        }

        System.out.println("\nSelect Role:");
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
            System.out.println("Invalid role choice. Registration cancelled.");
            return;
        }

        if (userDao.registerUser(username, password, role)) {
            System.out.println("Registration successful! Your account is currently PENDING approval.");
        } else {
            System.out.println("Registration failed. Username might already be taken.");
        }
    }
}