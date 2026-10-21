package com.grievanceportal.controller;

import com.grievanceportal.model.User;
import com.grievanceportal.service.GrievanceService;
import com.grievanceportal.service.Overdue;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/grievances/mine")
public class MyGrievancesServlet extends HttpServlet {

    private final GrievanceService service = new GrievanceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        try {
            request.setAttribute("rows", Overdue.flag(service.listOwn(user)));
        } catch (SQLException e) {
            throw new ServletException("Could not load your grievances", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/mine.jsp").forward(request, response);
    }
}
