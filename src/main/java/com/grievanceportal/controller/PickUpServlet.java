package com.grievanceportal.controller;

import com.grievanceportal.model.User;
import com.grievanceportal.service.GrievanceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.NoSuchElementException;

/** An officer picks up a Submitted grievance. AuthFilter keeps everyone else out. */
@WebServlet("/grievances/pickup")
public class PickUpServlet extends HttpServlet {

    private final GrievanceService service = new GrievanceService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
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
            service.pickUp(user, id);
            response.sendRedirect(request.getContextPath() + "/grievances/view?id=" + id + "&updated=1");
        } catch (NoSuchElementException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Grievance not found.");
        } catch (IllegalStateException e) {
            DetailPage.show(request, response, service, id, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not pick up the grievance", e);
        }
    }
}
