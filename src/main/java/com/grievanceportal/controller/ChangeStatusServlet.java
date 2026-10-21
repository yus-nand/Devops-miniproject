package com.grievanceportal.controller;

import com.grievanceportal.model.Status;
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

/**
 * Resolve, reject, close or reopen a grievance. The workflow rules live in the service, so a hand-made
 * request cannot do more than the buttons allow. AuthFilter keeps policyholders out.
 */
@WebServlet("/grievances/status")
public class ChangeStatusServlet extends HttpServlet {

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
        String note = request.getParameter("note");
        Status to = parseStatus(request.getParameter("to"));
        if (to == null) {
            request.setAttribute("noteText", note);
            DetailPage.show(request, response, service, id, HttpServletResponse.SC_BAD_REQUEST,
                    "Choose what to do with this grievance.");
            return;
        }
        try {
            service.changeStatus(user, id, to, note);
            response.sendRedirect(request.getContextPath() + "/grievances/view?id=" + id + "&updated=1");
        } catch (NoSuchElementException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Grievance not found.");
        } catch (IllegalArgumentException e) {
            request.setAttribute("noteText", note);
            DetailPage.show(request, response, service, id, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            request.setAttribute("noteText", note);
            DetailPage.show(request, response, service, id, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not change the status", e);
        }
    }

    private static Status parseStatus(String code) {
        for (Status status : Status.values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
