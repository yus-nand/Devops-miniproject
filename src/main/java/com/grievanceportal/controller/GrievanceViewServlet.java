package com.grievanceportal.controller;

import com.grievanceportal.model.GrievanceDetail;
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

@WebServlet("/grievances/view")
public class GrievanceViewServlet extends HttpServlet {

    private final GrievanceService service = new GrievanceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        long id;
        try {
            id = Long.parseLong(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Grievance not found.");
            return;
        }
        try {
            Optional<GrievanceDetail> detail = service.findDetail(id, user);
            if (detail.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Grievance not found.");
                return;
            }
            request.setAttribute("detail", detail.get());
            request.getRequestDispatcher("/WEB-INF/views/detail.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the grievance", e);
        }
    }
}
