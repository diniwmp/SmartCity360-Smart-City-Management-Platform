package com.urbantech.smartcity360.filter;

import com.urbantech.smartcity360.util.AppConstants;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getServletPath();

        if (!path.startsWith(AppConstants.ASSETS_PREFIX)) {
            disableCaching(resp);
        }

        if (AppConstants.isPublicPath(path) || isLoggedIn(req)) {
            chain.doFilter(request, response);
            return;
        }
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_LOGIN_PAGE);
    }

    private boolean isLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && session.getAttribute(AppConstants.SESSION_USER) != null;
    }

    private void disableCaching(HttpServletResponse resp) {
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
    }
}