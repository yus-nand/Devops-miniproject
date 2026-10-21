package com.grievanceportal.controller;

import com.grievanceportal.model.User;
import com.grievanceportal.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getSession().getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        try {
            Optional<User> user = authService.login(username, password);
            if (user.isPresent()) {
                request.changeSessionId();
                request.getSession().setAttribute("user", user.get());
                response.sendRedirect(request.getContextPath() + "/home");
                return;
            }
        } catch (SQLException e) {
            throw new ServletException("Login failed because of a database error", e);
        }
        request.setAttribute("error", "Invalid username or password.");
        request.setAttribute("username", username);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }
}
