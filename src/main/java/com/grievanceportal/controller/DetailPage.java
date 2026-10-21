package com.grievanceportal.controller;

import com.grievanceportal.model.GrievanceDetail;
import com.grievanceportal.model.User;
import com.grievanceportal.service.GrievanceService;
import com.grievanceportal.service.Overdue;
import com.grievanceportal.service.Workflow;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/** Renders the detail page. Shared by the view servlet and by the action servlets when an action is refused. */
final class DetailPage {

    private DetailPage() {
    }

    /**
     * Shows the page with the given HTTP status. A non-null message is shown as an error above the actions.
     * Sends a 404 instead when the grievance does not exist or this user may not see it.
     */
    static void show(HttpServletRequest request, HttpServletResponse response, GrievanceService service,
            long id, int httpStatus, String errorMessage) throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        try {
            Optional<GrievanceDetail> detail = service.findDetail(id, user);
            if (detail.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Grievance not found.");
                return;
            }
            request.setAttribute("detail", detail.get());
            request.setAttribute("actions", Workflow.actionsFor(detail.get().getGrievance(), user));
            request.setAttribute("overdue", Overdue.isOverdue(detail.get().getGrievance(), LocalDate.now()));
            if (errorMessage != null) {
                request.setAttribute("actionError", errorMessage);
            }
            response.setStatus(httpStatus);
            request.getRequestDispatcher("/WEB-INF/views/detail.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the grievance", e);
        }
    }
}
