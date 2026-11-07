package com.scc;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.CustomerDao;
import com.scc.dao.DatabaseConnection;
import com.scc.dao.DatabaseInitializer;
import com.scc.dao.OrderDao;
import com.scc.dao.PrintingServiceDao;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        try {
            Connection connection = DatabaseConnection.getConnection();

            ActivityLogDao activityLogDao = new ActivityLogDao();
            UserDao userDao = new UserDao(connection, activityLogDao);
            PrintingServiceDao printingServiceDao = new PrintingServiceDao(connection, activityLogDao);
            CustomerDao customerDao = new CustomerDao(connection, activityLogDao);
            OrderDao orderDao = new OrderDao(connection, activityLogDao);
            Scanner scanner = new Scanner(System.in);

            new Authentication(userDao, activityLogDao, printingServiceDao,
                    customerDao, orderDao, scanner).start();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}