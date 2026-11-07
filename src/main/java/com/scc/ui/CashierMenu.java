package com.scc.ui;

import com.scc.dao.CustomerDao;
import com.scc.dao.OrderDao;
import com.scc.dao.PrintingServiceDao;
import com.scc.model.Customer;
import com.scc.model.PrintingService;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class CashierMenu {
    private final User user;
    private final OrderDao orderDao;
    private final PrintingServiceDao printingServiceDao;
    private final CustomerDao customerDao;
    private final Scanner scanner;

    public CashierMenu(User user, OrderDao orderDao, PrintingServiceDao printingServiceDao,
                       CustomerDao customerDao, Scanner scanner) {
        this.user = user;
        this.orderDao = orderDao;
        this.printingServiceDao = printingServiceDao;
        this.customerDao = customerDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean active = true;
        while (active) {
            System.out.println("\n========================================");
            System.out.println("         PRINTFLOW - CASHIER MENU");
            System.out.println("========================================");
            System.out.println("1. Create Printing Order & Record Payment");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> createOrderFlow();
                case "0" -> {
                    active = false;
                    System.out.println("Logout successful.");
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // Returns a valid customer ID, or -1 if cancelled / failed
    private int chooseCustomer() {
        List<Customer> customers = customerDao.getAllCustomers();
        if (!customers.isEmpty()) {
            String border = "+" + "-".repeat(6) + "+" + "-".repeat(22) + "+" + "-".repeat(16) + "+";
            String format = "| %-4s | %-20s | %-14s |%n";
            System.out.println("\n--- CUSTOMERS ---");
            System.out.println(border);
            System.out.printf(format, "ID", "NAME", "CONTACT");
            System.out.println(border);
            for (Customer c : customers) {
                System.out.printf(format, c.id(), c.customerName(), c.contactNumber());
            }
            System.out.println(border);
        } else {
            System.out.println("No customers yet. Please add one.");
        }

        System.out.print("Enter Customer ID (N = add new customer, 0 = cancel): ");
        String input = scanner.nextLine().trim();

        if (input.equals("0")) return -1;
        if (input.equalsIgnoreCase("N")) return addCustomerFlow();

        int parsed;
        try {
            parsed = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Operation cancelled.");
            return -1;
        }

        final int customerId = parsed;
        if (customers.stream().noneMatch(c -> c.id() == customerId)) {
            System.out.println("Selected Customer ID does not exist.");
            return -1;
        }
        return customerId;
    }

    private int addCustomerFlow() {
        System.out.print("Customer name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Contact number: ");
        String contact = scanner.nextLine().trim();
        System.out.print("Address: ");
        String address = scanner.nextLine().trim();

        if (name.isEmpty() || contact.isEmpty() || address.isEmpty()) {
            System.out.println("All customer fields are required. Operation cancelled.");
            return -1;
        }

        int newId = customerDao.addCustomer(user.id(), name, contact, address);
        if (newId > 0) {
            System.out.println("Customer added with ID " + newId + ".");
        } else {
            System.out.println("Failed to add customer.");
        }
        return newId;
    }

    private void createOrderFlow() {
        int customerId = chooseCustomer();
        if (customerId <= 0) return;

        System.out.println("\n--- AVAILABLE PRINTING SERVICES ---");
        List<PrintingService> services = printingServiceDao.getAllServices().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.status()))
                .toList();
        if (services.isEmpty()) {
            System.out.println("No active services available at the moment.");
            return;
        }

        String border = "+" + "-".repeat(6) + "+" + "-".repeat(25) + "+" + "-".repeat(10) + "+";
        String format = "| %-4s | %-23s | %-8s |%n";
        System.out.println(border);
        System.out.printf(format, "ID", "SERVICE NAME", "PRICE");
        System.out.println(border);
        for (PrintingService s : services) {
            System.out.printf(format, s.id(), s.serviceName(), s.price());
        }
        System.out.println(border);

        System.out.print("Enter Service ID to order: ");
        int parsedServiceId;
        try {
            parsedServiceId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Operation cancelled.");
            return;
        }

        final int serviceId = parsedServiceId;
        PrintingService selectedService = services.stream()
                .filter(s -> s.id() == serviceId)
                .findFirst()
                .orElse(null);

        if (selectedService == null) {
            System.out.println("Selected Service ID does not exist.");
            return;
        }

        System.out.print("Enter quantity: ");
        int quantity;
        try {
            quantity = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity format!");
            return;
        }
        if (quantity <= 0) {
            System.out.println("Quantity must be greater than zero.");
            return;
        }

        double totalPrice = selectedService.price() * quantity;
        System.out.println("Total Price to Pay: P" + String.format("%.2f", totalPrice));

        System.out.print("Confirm and record payment? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("y") || confirm.equals("yes")) {
            if (orderDao.createOrder(user.id(), customerId, selectedService.id(), quantity, totalPrice)) {
                System.out.println("Order created and payment recorded successfully!");
            } else {
                System.out.println("Failed to record order in the database.");
            }
        } else {
            System.out.println("Order transaction cancelled.");
        }
    }
}