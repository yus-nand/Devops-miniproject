package com.grievanceportal.controller;

import com.grievanceportal.util.AppConfig;
import com.grievanceportal.util.AppInfo;
import com.grievanceportal.util.Database;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Reports whether the application and its database are working. */
@WebServlet("/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean up = databaseIsUp();
        resp.setStatus(up ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().print(buildJson(up, AppInfo.version(), AppConfig.envName()));
    }

    static String buildJson(boolean up, String version, String environment) {
        return "{\"status\":\"" + (up ? "UP" : "DOWN") + "\","
                + "\"version\":\"" + escape(version) + "\","
                + "\"environment\":\"" + escape(environment) + "\"}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean databaseIsUp() {
        try (Connection c = Database.getConnection(); Statement s = c.createStatement()) {
            s.execute("SELECT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}