package com.urbantech.smartcity360.filter;

import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.model.enums.AppModule;
import com.urbantech.smartcity360.util.AppConstants;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

public class RoleAuthorizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        LoggedInUser user = currentUser(req);
        if (user != null) {
            Optional<AppModule> module = AppModule.findByPath(req.getServletPath());
            if (module.isPresent() && !module.get().isAllowedFor(user.getRole())) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Access Denied");
                req.getRequestDispatcher(AppConstants.VIEW_ACCESS_DENIED).forward(req, resp);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private LoggedInUser currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (LoggedInUser) session.getAttribute(AppConstants.SESSION_USER);
    }
}