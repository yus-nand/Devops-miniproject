package com.grievanceportal;

import com.grievanceportal.util.AppConfig;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.SQLException;

/** Prepares the database when the application starts. */
@WebListener
public class AppInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Schema.init();
            Seeder.seedUsers();
            event.getServletContext().log("Database ready: " + AppConfig.dbUrl());
        } catch (SQLException e) {
            throw new IllegalStateException("Database initialisation failed", e);
        }
    }
}