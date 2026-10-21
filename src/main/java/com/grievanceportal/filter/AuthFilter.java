package com.grievanceportal.filter;

import com.grievanceportal.util.AppConfig;
import com.grievanceportal.util.Csrf;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/** Requires login for every page except login, health and static files, and checks CSRF tokens on POST. */
@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getServletPath();

        if (path.equals("/health") || path.startsWith("/css/")) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(true);
        request.setAttribute("csrfToken", Csrf.tokenFor(session));
        request.setAttribute("envName", AppConfig.envName());

        if ("POST".equalsIgnoreCase(request.getMethod()) && !Csrf.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing security token.");
            return;
        }

        boolean loggedIn = session.getAttribute("user") != null;
        if (!loggedIn && !path.equals("/login")) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        chain.doFilter(req, res);
    }
}
