package com.urbantech.smartcity360.controller;

import com.urbantech.smartcity360.dto.AddBinsForm;
import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ResourceNotFoundException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.CollectionRouteEntity;
import com.urbantech.smartcity360.model.enums.BinStatus;
import com.urbantech.smartcity360.service.waste.WasteManagementService;
import com.urbantech.smartcity360.util.AppConstants;
import com.urbantech.smartcity360.util.FlashMessages;
import com.urbantech.smartcity360.util.InputParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "WasteManagementServlet", urlPatterns = AppConstants.PATH_WASTE)
public class WasteManagementServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(WasteManagementServlet.class.getName());
    private static final String SYSTEM_ERROR = "System error. Please try again later.";

    private static final String ACTION_ADD_BINS = "addBins";
    private static final String ACTION_SIMULATE = "simulate";
    private static final String ACTION_PLAN_ROUTE = "planRoute";
    private static final String ACTION_COLLECT = "collect";
    private static final String ACTION_COMPLETE_ROUTE = "completeRoute";
    private static final Set<String> MANAGER_ACTIONS = Set.of(ACTION_ADD_BINS, ACTION_SIMULATE, ACTION_PLAN_ROUTE);

    private final WasteManagementService wasteService = new WasteManagementService();

    @FunctionalInterface
    private interface WasteAction {
        String perform() throws ResourceNotFoundException, ValidationException;
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        FlashMessages.moveToRequest(req);
        renderPage(req, resp);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action") == null ? "" : req.getParameter("action");

        if (MANAGER_ACTIONS.contains(action) && !wasteService.canManage(currentUser(req).getRole())) {
            denyAccess(req, resp);
            return;
        }

        switch (action) {
            case ACTION_COLLECT -> runAction(req, resp, returnSuffix(req), () -> {
                wasteService.collectBin(InputParser.parseLong(req.getParameter("binId")).orElse(null));
                return "Bin collected and emptied.";
            });
            case ACTION_COMPLETE_ROUTE -> runAction(req, resp, "#routes", () -> {
                int collected = wasteService.completeRoute(InputParser.parseLong(req.getParameter("routeId")).orElse(null));
                return "Route completed. " + collected + " bins were emptied.";
            });
            case ACTION_SIMULATE -> runAction(req, resp, "#bins", () -> {
                int updated = wasteService.simulateFillUp(AppConstants.WASTE_FILL_STEP_MAX);
                return "Fill levels updated for " + updated + " bins.";
            });
            case ACTION_PLAN_ROUTE -> runAction(req, resp, "#routes", () -> {
                CollectionRouteEntity route = wasteService.planRoute();
                return "Route planned: " + route.getRouteName() + " (" + route.getBins().size() + " bins).";
            });
            case ACTION_ADD_BINS -> addBins(req, resp);
            default -> redirect(req, resp, AppConstants.FLASH_ERROR, "Unknown action.", "");
        }
    }

    private void addBins(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AddBinsForm form = new AddBinsForm();
        form.setTemplate(req.getParameter("template"));
        form.setZoneId(req.getParameter("zoneId"));
        form.setQuantity(req.getParameter("quantity"));

        try {
            int added = wasteService.addBins(form);
            redirect(req, resp, AppConstants.FLASH_SUCCESS, added + " bins added.", "#bins");
        } catch (ValidationException e) {
            req.setAttribute(AppConstants.ATTR_ADD_BINS_FORM, form);
            req.setAttribute(AppConstants.ATTR_ADD_BINS_ERRORS, e.getErrors());
            renderPage(req, resp);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Adding bins failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, "");
        }
    }

    private void runAction(HttpServletRequest req, HttpServletResponse resp, String suffix, WasteAction action)
            throws IOException {
        try {
            redirect(req, resp, AppConstants.FLASH_SUCCESS, action.perform(), suffix);
        } catch (ResourceNotFoundException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, e.getMessage(), suffix);
        } catch (ValidationException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, String.join(" ", e.getErrors()), suffix);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Waste action failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, suffix);
        }
    }


    private void renderPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean canManage = wasteService.canManage(currentUser(req).getRole());
        String statusFilter = req.getParameter("status");
        Long streetId = InputParser.parseLong(req.getParameter("street")).orElse(null);
        int page = InputParser.parseLong(req.getParameter("page")).orElse(1L).intValue();

        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Waste Management");
        req.setAttribute(AppConstants.ATTR_BIN_OVERVIEW, wasteService.getOverview());
        req.setAttribute(AppConstants.ATTR_BIN_PAGE, wasteService.getPage(statusFilter, streetId, page));
        req.setAttribute(AppConstants.ATTR_ROUTE_ROWS, wasteService.getRoutes());
        req.setAttribute(AppConstants.ATTR_STREET_ZONES, wasteService.getStreetZones());
        req.setAttribute(AppConstants.ATTR_BIN_STATUSES, BinStatus.values());
        req.setAttribute(AppConstants.ATTR_CAN_MANAGE_WASTE, canManage);
        if (canManage) {
            req.setAttribute(AppConstants.ATTR_BIN_TEMPLATES, wasteService.getTemplateOptions());
            if (req.getAttribute(AppConstants.ATTR_ADD_BINS_FORM) == null) {
                AddBinsForm form = new AddBinsForm();
                form.setQuantity("10");
                req.setAttribute(AppConstants.ATTR_ADD_BINS_FORM, form);
            }
        }
        req.getRequestDispatcher(AppConstants.VIEW_WASTE).forward(req, resp);
    }

    private void denyAccess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Access Denied");
        req.getRequestDispatcher(AppConstants.VIEW_ACCESS_DENIED).forward(req, resp);
    }


    private LoggedInUser currentUser(HttpServletRequest req) {
        return (LoggedInUser) req.getSession().getAttribute(AppConstants.SESSION_USER);
    }

    private String returnSuffix(HttpServletRequest req) {
        String status = req.getParameter("returnStatus");
        boolean validStatus = status != null && (AppConstants.WASTE_FILTER_NEEDS.equals(status)
                || Arrays.stream(BinStatus.values()).anyMatch(s -> s.name().equals(status)));
        String street = InputParser.parseLong(req.getParameter("returnStreet")).map(String::valueOf).orElse("");
        long page = InputParser.parseLong(req.getParameter("returnPage")).orElse(1L);
        return "?status=" + (validStatus ? status : "") + "&street=" + street + "&page=" + Math.max(1, page) + "#bins";
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String flashKey, String message,
                          String suffix) throws IOException {
        FlashMessages.set(req, flashKey, message);
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_WASTE + suffix);
    }
}