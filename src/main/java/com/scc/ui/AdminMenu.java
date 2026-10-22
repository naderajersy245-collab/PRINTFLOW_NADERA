package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class AdminMenu {

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
            System.out.println("2. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.println("\n--- ACCOUNT INFORMATION ---");
                    System.out.println("ID: " + user.id());
                    System.out.println("Username: " + user.username());
                    System.out.println("Role: " + user.roles());
                    System.out.println("Status: " + user.status());
                }
                case "2" -> {
                    loggedIn = false;
                    System.out.println("Logout successful.");
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}