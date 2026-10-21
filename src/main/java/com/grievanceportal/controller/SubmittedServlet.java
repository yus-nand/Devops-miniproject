package com.grievanceportal.controller;

import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.User;
import com.grievanceportal.service.GrievanceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet("/grievances/submitted")
public class SubmittedServlet extends HttpServlet {

    private final GrievanceService service = new GrievanceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        try {
            Optional<Grievance> grievance = service.findOwn(request.getParameter("ref"), user);
            if (grievance.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Grievance not found.");
                return;
            }
            request.setAttribute("grievance", grievance.get());
            request.getRequestDispatcher("/WEB-INF/views/submitted.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the grievance", e);
        }
    }
}
