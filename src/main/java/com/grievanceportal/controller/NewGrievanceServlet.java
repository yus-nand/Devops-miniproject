package com.grievanceportal.controller;

import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.User;
import com.grievanceportal.service.GrievanceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/grievances/new")
public class NewGrievanceServlet extends HttpServlet {

    private final GrievanceService service = new GrievanceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showForm(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        GrievanceForm form = new GrievanceForm(
                request.getParameter("policyNumber"),
                request.getParameter("category"),
                request.getParameter("title"),
                request.getParameter("description"));

        Map<String, String> errors = service.validate(form);
        if (!errors.isEmpty()) {
            request.setAttribute("form", form);
            request.setAttribute("errors", errors);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showForm(request, response);
            return;
        }

        try {
            Grievance saved = service.submit(user, form);
            String ref = URLEncoder.encode(saved.getRefNo(), StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/grievances/submitted?ref=" + ref);
        } catch (SQLException e) {
            throw new ServletException("Could not save the grievance", e);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("categories", Category.values());
        request.getRequestDispatcher("/WEB-INF/views/new.jsp").forward(request, response);
    }
}
