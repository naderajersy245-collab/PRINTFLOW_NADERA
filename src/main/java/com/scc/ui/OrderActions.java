package com.scc.ui;

import com.scc.dao.OrderDao;
import com.scc.dao.PrintingServiceDao;
import com.scc.model.Order;
import com.scc.model.PrintingService;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class OrderActions {
    private static final String BORDER = "+" + "-".repeat(6) + "+" + "-".repeat(13) + "+" + "-".repeat(22)
            + "+" + "-".repeat(10) + "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(21) + "+";
    private static final String FORMAT = "| %-4s | %-11s | %-20s | %-8s | %-12s | %-12s | %-19s |%n";

    private final User user;
    private final OrderDao orderDao;
    private final PrintingServiceDao printingServiceDao;
    private final Scanner scanner;

    public OrderActions(User user, OrderDao orderDao, PrintingServiceDao printingServiceDao, Scanner scanner) {
        this.user = user;
        this.orderDao = orderDao;
        this.printingServiceDao = printingServiceDao;
        this.scanner = scanner;
    }

    public static void printOrders(List<Order> orders) {
        System.out.println(BORDER);
        System.out.printf(FORMAT, "ID", "CUSTOMER ID", "SERVICE NAME", "QTY", "TOTAL PRICE", "STATUS", "CREATED AT");
        System.out.println(BORDER);
        for (Order o : orders) {
            System.out.printf(FORMAT, o.id(), o.customerId(), o.serviceName(), o.quantity(),
                    String.format("%.2f", o.totalPrice()), o.status(), o.createdAt());
        }
        System.out.println(BORDER);
    }

    public void viewOrders() {
        System.out.println("\n--- PRINTING ORDERS ---");
        List<Order> orders = orderDao.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No printing orders found.");
            return;
        }
        printOrders(orders);
    }

    public void updateOrderInfoFlow() {
        List<Order> orders = orderDao.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No printing orders found.");
            return;
        }
        printOrders(orders);

        System.out.print("Enter Order ID to update (0 to cancel): ");
        int orderId;
        try {
            orderId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input!");
            return;
        }
        if (orderId == 0) return;

        final int selectedOrderId = orderId;
        Order order = orders.stream().filter(o -> o.id() == selectedOrderId).findFirst().orElse(null);
        if (order == null) {
            System.out.println("Order ID does not exist.");
            return;
        }
        if (!"PENDING".equals(order.status())) {
            System.out.println("Only PENDING orders can be edited. This order is " + order.status() + ".");
            return;
        }

        List<PrintingService> services = printingServiceDao.getAllServices().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.status()))
                .toList();
        if (services.isEmpty()) {
            System.out.println("No active services available.");
            return;
        }
        System.out.println("\n--- ACTIVE SERVICES ---");
        for (PrintingService s : services) {
            System.out.printf("%d. %s - P%.2f%n", s.id(), s.serviceName(), s.price());
        }

        System.out.print("Enter new Service ID: ");
        int serviceId;
        try {
            serviceId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input!");
            return;
        }
        final int sid = serviceId;
        PrintingService service = services.stream().filter(s -> s.id() == sid).findFirst().orElse(null);
        if (service == null) {
            System.out.println("Selected Service ID is not available.");
            return;
        }

        System.out.print("Enter new quantity: ");
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

        double total = service.price() * quantity;
        System.out.println("New Total Price: P" + String.format("%.2f", total));
        System.out.print("Save changes? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("y") && !confirm.equals("yes")) {
            System.out.println("Update cancelled.");
            return;
        }

        if (orderDao.updateOrderInfo(user.id(), orderId, service.id(), quantity, total)) {
            System.out.println("Order updated successfully.");
        } else {
            System.out.println("Update failed. Only PENDING orders can be edited.");
        }
    }

    public void cancelOrderFlow() {
        List<Order> orders = orderDao.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No printing orders found.");
            return;
        }
        printOrders(orders);

        System.out.print("Enter Order ID to cancel (0 to go back): ");
        int orderId;
        try {
            orderId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input!");
            return;
        }
        if (orderId == 0) return;

        System.out.print("Are you sure you want to cancel Order #" + orderId + "? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("y") && !confirm.equals("yes")) {
            System.out.println("Cancellation aborted.");
            return;
        }

        if (orderDao.cancelOrder(user.id(), orderId)) {
            System.out.println("Order #" + orderId + " cancelled.");
        } else {
            System.out.println("Cancel failed. Only existing PENDING orders can be cancelled.");
        }
    }
}