package com.grievanceportal.controller;

import com.grievanceportal.util.AppConfig;
import com.grievanceportal.util.Csrf;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Shows a friendly error page. web.xml sends error status codes and uncaught exceptions here.
 * The page never shows the exception or a stack trace. Details go to the server log only.
 */
@WebServlet("/error")
public class ErrorServlet extends HttpServlet {

    private static final String STATUS_ATTRIBUTE = "jakarta.servlet.error.status_code";
    private static final String EXCEPTION_ATTRIBUTE = "jakarta.servlet.error.exception";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer status = (Integer) request.getAttribute(STATUS_ATTRIBUTE);
        if (status == null) {
            // Someone opened /error directly rather than being sent here by an error.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Object cause = request.getAttribute(EXCEPTION_ATTRIBUTE);
        if (cause instanceof Throwable) {
            getServletContext().log("Unhandled error", (Throwable) cause);
        }

        // Error pages skip the auth filter, so supply what the shared layout needs.
        HttpSession session = request.getSession(false);
        request.setAttribute("csrfToken", session == null ? "" : Csrf.tokenFor(session));
        request.setAttribute("envName", AppConfig.envName());
        request.setAttribute("errorStatus", status);
        request.setAttribute("errorTitle", titleFor(status));
        request.setAttribute("errorMessage", messageFor(status));
        request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
    }

    static String titleFor(int status) {
        switch (status) {
            case 400:
                return "Bad request";
            case 403:
                return "Access denied";
            case 404:
                return "Page not found";
            case 405:
                return "Action not supported";
            default:
                return "Something went wrong";
        }
    }

    static String messageFor(int status) {
        switch (status) {
            case 400:
                return "The request could not be understood. Please go back and try again.";
            case 403:
                return "You do not have access to this page or action.";
            case 404:
                return "We could not find the page you asked for. It may have moved, or the link may be wrong.";
            case 405:
                return "That action is not supported on this page.";
            default:
                return "The portal ran into a problem. Please try again in a moment. "
                        + "If it keeps happening, contact the administrator.";
        }
    }
}
