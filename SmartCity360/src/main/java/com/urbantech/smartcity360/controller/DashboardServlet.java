package com.urbantech.smartcity360.controller;

import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.service.dashboard.DashboardService;
import com.urbantech.smartcity360.util.AppConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "DashboardServlet", urlPatterns = AppConstants.PATH_DASHBOARD)
public class DashboardServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(DashboardServlet.class.getName());

    private final DashboardService dashboardService = new DashboardService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LoggedInUser user = (LoggedInUser) req.getSession().getAttribute(AppConstants.SESSION_USER);

        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Dashboard");
        try {
            req.setAttribute(AppConstants.ATTR_DASHBOARD, dashboardService.build(user));
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Building the dashboard summary failed", e);
        }
        req.getRequestDispatcher(AppConstants.VIEW_DASHBOARD).forward(req, resp);
    }
}