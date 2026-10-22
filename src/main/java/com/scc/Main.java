package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // 1. Initialize database
        DatabaseInitializer.initialize();

        // 2. Instantiate shared DAOs & utilities
        UserDao userDao = new UserDao();
        Scanner scanner = new Scanner(System.in);

        // 3. Delegate execution
        Authentication authentication = new Authentication(userDao, scanner);
        authentication.start();

        scanner.close();
    }
}