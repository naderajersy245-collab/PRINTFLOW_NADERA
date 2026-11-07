package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {

        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING'
                )
                """;

        String createCustomersTable = """
                CREATE TABLE IF NOT EXISTS customers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_name TEXT NOT NULL,
                    contact_number TEXT NOT NULL,
                    address TEXT NOT NULL
                )
                """;

        String createPrintingServicesTable = """
                CREATE TABLE IF NOT EXISTS printing_services (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    service_name TEXT NOT NULL UNIQUE,
                    description TEXT NOT NULL DEFAULT '',
                    price REAL NOT NULL CHECK (price >= 0),
                    service_status TEXT NOT NULL DEFAULT 'ACTIVE'
                        CHECK (service_status IN ('ACTIVE', 'INACTIVE'))
                )
                """;

        String createOrdersTable = """
                CREATE TABLE IF NOT EXISTS orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    service_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    total_amount REAL NOT NULL CHECK (total_amount >= 0),
                    order_status TEXT NOT NULL DEFAULT 'PENDING'
                        CHECK (order_status IN ('PENDING', 'PRINTING', 'READY', 'COMPLETED', 'CANCELLED')),
                    payment_status TEXT NOT NULL DEFAULT 'UNPAID'
                        CHECK (payment_status IN ('UNPAID', 'PARTIAL', 'PAID')),
                    amount_paid REAL NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
                    order_date TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
                    FOREIGN KEY (service_id) REFERENCES printing_services(id) ON DELETE RESTRICT
                )
                """;

        String createActivityLogsTable = """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """;

        String seedAdmin = """
                INSERT INTO users (username, password, roles, status)
                SELECT 'admin', 'admin123', 'ADMIN', 'APPROVED'
                WHERE NOT EXISTS (SELECT 1 FROM users)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createUsersTable);
            statement.execute(createCustomersTable);
            statement.execute(createPrintingServicesTable);
            statement.execute(createOrdersTable);
            statement.execute(createActivityLogsTable);

            statement.execute("CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_orders_service ON orders(service_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(order_status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_orders_date ON orders(order_date)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_logs_user ON activity_logs(user_id)");

            statement.execute(seedAdmin);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.out.println("Database initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }
}