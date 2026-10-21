package com.grievanceportal.controller;

import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceForm;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/** Lets a policyholder correct their own grievance until an officer starts review. */
@WebServlet("/grievances/edit")
public class EditGrievanceServlet extends HttpServlet {

    private final GrievanceService service = new GrievanceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Grievance grievance = loadEditable(request, response, request.getParameter("id"));
        if (grievance == null) {
            return;
        }
        GrievanceForm form = new GrievanceForm(grievance.getPolicyNumber(), grievance.getCategory().getCode(),
                grievance.getTitle(), grievance.getDescription());
        showForm(request, response, grievance, form);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Grievance grievance = loadEditable(request, response, request.getParameter("id"));
        if (grievance == null) {
            return;
        }
        User user = (User) request.getSession().getAttribute("user");
        GrievanceForm form = new GrievanceForm(
                request.getParameter("policyNumber"),
                request.getParameter("category"),
                request.getParameter("title"),
                request.getParameter("description"));

        Map<String, String> errors = service.validate(form);
        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showForm(request, response, grievance, form);
            return;
        }
        try {
            service.update(user, grievance.getId(), form);
            response.sendRedirect(request.getContextPath() + "/grievances/view?id=" + grievance.getId()
                    + "&saved=1");
        } catch (NoSuchElementException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Grievance not found.");
        } catch (IllegalStateException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "This grievance can no longer be edited.");
        } catch (SQLException e) {
            throw new ServletException("Could not save the grievance", e);
        }
    }

    /** Returns the grievance if the user may edit it, otherwise sends the error response and returns null. */
    private Grievance loadEditable(HttpServletRequest request, HttpServletResponse response, String idText)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        long id;
        try {
            id = Long.parseLong(idText);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Grievance not found.");
            return null;
        }
        try {
            Optional<Grievance> found = service.findOwnById(id, user);
            if (found.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Grievance not found.");
                return null;
            }
            if (found.get().getStatus() != Status.SUBMITTED) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "This grievance can no longer be edited.");
                return null;
            }
            return found.get();
        } catch (SQLException e) {
            throw new ServletException("Could not load the grievance", e);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Grievance grievance,
            GrievanceForm form) throws ServletException, IOException {
        request.setAttribute("grievance", grievance);
        request.setAttribute("form", form);
        request.setAttribute("categories", Category.values());
        request.getRequestDispatcher("/WEB-INF/views/edit.jsp").forward(request, response);
    }
}
