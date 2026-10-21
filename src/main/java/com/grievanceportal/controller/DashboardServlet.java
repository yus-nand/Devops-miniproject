package com.grievanceportal.controller;

import com.grievanceportal.service.DashboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** The admin summary dashboard. AuthFilter keeps everyone else out. */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private final DashboardService service = new DashboardService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("summary", service.summarize());
        } catch (SQLException e) {
            throw new ServletException("Could not load the dashboard", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
    }
}
