package com.grievanceportal.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens connections to the H2 database. */
public final class Database {

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(AppConfig.dbUrl(), "sa", "");
    }
}