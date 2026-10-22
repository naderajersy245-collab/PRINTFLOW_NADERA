package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:printflow.db";

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);

        // Enable foreign key constraints on every connection
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }

        return connection;
    }
}