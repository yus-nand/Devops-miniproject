package com.grievanceportal.controller;

import com.grievanceportal.model.Category;
import com.grievanceportal.model.QueueFilter;
import com.grievanceportal.model.Status;
import com.grievanceportal.service.StaffQueueService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** All grievances for officers and admins. AuthFilter keeps policyholders out. */
@WebServlet("/grievances")
public class StaffQueueServlet extends HttpServlet {

    private final StaffQueueService service = new StaffQueueService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        QueueFilter filter = QueueFilter.of(
                request.getParameter("q"), request.getParameter("status"), request.getParameter("category"),
                "1".equals(request.getParameter("overdue")));
        try {
            request.setAttribute("rows", service.queue(filter));
        } catch (SQLException e) {
            throw new ServletException("Could not load the grievances", e);
        }
        request.setAttribute("filter", filter);
        request.setAttribute("statuses", Status.values());
        request.setAttribute("categories", Category.values());
        request.getRequestDispatcher("/WEB-INF/views/queue.jsp").forward(request, response);
    }
}
