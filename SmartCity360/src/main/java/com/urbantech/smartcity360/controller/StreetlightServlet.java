package com.urbantech.smartcity360.controller;

import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ResourceNotFoundException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.enums.DeviceStatus;
import com.urbantech.smartcity360.service.lighting.LightingGridService;
import com.urbantech.smartcity360.service.lighting.MemoryComparison;
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

@WebServlet(name = "StreetlightServlet", urlPatterns = AppConstants.PATH_STREETLIGHTS)
public class StreetlightServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(StreetlightServlet.class.getName());
    private static final String SYSTEM_ERROR = "System error. Please try again later.";

    private static final String ACTION_SET_STATUS = "setStatus";
    private static final String ACTION_SET_STREET = "setStreet";
    private static final String ACTION_INSTALL = "install";
    private static final String ACTION_BENCHMARK = "benchmark";
    private static final Set<String> MANAGER_ACTIONS = Set.of(ACTION_SET_STREET, ACTION_INSTALL, ACTION_BENCHMARK);

    private final LightingGridService lightService = new LightingGridService();

    @FunctionalInterface
    private interface LightAction {
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

        if (MANAGER_ACTIONS.contains(action) && !lightService.canManage(currentUser(req).getRole())) {
            denyAccess(req, resp);
            return;
        }

        switch (action) {
            case ACTION_SET_STATUS -> runAction(req, resp, returnSuffix(req), () -> {
                lightService.changeStatus(InputParser.parseLong(req.getParameter("lightId")).orElse(null),
                        req.getParameter("status"));
                return "Street light updated.";
            });
            case ACTION_SET_STREET -> runAction(req, resp, returnSuffix(req), () -> {
                int changed = lightService.changeStreetStatus(
                        InputParser.parseLong(req.getParameter("zoneId")).orElse(null), req.getParameter("status"));
                return String.format("%,d lights updated.", changed);
            });
            case ACTION_INSTALL -> runAction(req, resp, "#lights", () -> {
                int installed = lightService.installLights(parseCount(req));
                return String.format("%,d street lights added.", installed);
            });
            case ACTION_BENCHMARK -> runAction(req, resp, "#memory", () -> {
                MemoryComparison result = lightService.compareMemory(parseCount(req));
                req.getSession().setAttribute(AppConstants.SESSION_MEMORY_RESULT, result);
                return String.format("Memory test finished for %,d lights.", result.getLightCount());
            });
            default -> redirect(req, resp, AppConstants.FLASH_ERROR, "Unknown action.", "");
        }
    }

    private void runAction(HttpServletRequest req, HttpServletResponse resp, String suffix, LightAction action)
            throws IOException {
        try {
            redirect(req, resp, AppConstants.FLASH_SUCCESS, action.perform(), suffix);
        } catch (ResourceNotFoundException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, e.getMessage(), suffix);
        } catch (ValidationException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, String.join(" ", e.getErrors()), suffix);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Street light action failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, suffix);
        }
    }


    private void renderPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean canManage = lightService.canManage(currentUser(req).getRole());
        String statusFilter = req.getParameter("status");
        Long streetId = InputParser.parseLong(req.getParameter("street")).orElse(null);
        int page = InputParser.parseLong(req.getParameter("page")).orElse(1L).intValue();

        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Street Lights");
        req.setAttribute(AppConstants.ATTR_LIGHT_OVERVIEW, lightService.getOverview());
        req.setAttribute(AppConstants.ATTR_LIGHT_PAGE, lightService.getPage(statusFilter, streetId, page));
        req.setAttribute(AppConstants.ATTR_STREET_ZONES, lightService.getStreetZones());
        req.setAttribute(AppConstants.ATTR_SPEC_ROWS, lightService.getSpecRows());
        req.setAttribute(AppConstants.ATTR_LIGHT_STATUSES, DeviceStatus.values());
        req.setAttribute(AppConstants.ATTR_CAN_MANAGE_LIGHTS, canManage);
        if (canManage) {
            req.setAttribute(AppConstants.ATTR_INSTALL_OPTIONS, AppConstants.LIGHT_INSTALL_OPTIONS);
            req.setAttribute(AppConstants.ATTR_MEMORY_OPTIONS, AppConstants.MEMORY_TEST_OPTIONS);
            req.setAttribute(AppConstants.ATTR_SCALING_ROWS, lightService.getScalingEstimate());
            req.setAttribute(AppConstants.ATTR_MEMORY_RESULT,
                    req.getSession().getAttribute(AppConstants.SESSION_MEMORY_RESULT));
        }
        req.getRequestDispatcher(AppConstants.VIEW_STREETLIGHTS).forward(req, resp);
    }

    private void denyAccess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Access Denied");
        req.getRequestDispatcher(AppConstants.VIEW_ACCESS_DENIED).forward(req, resp);
    }


    private LoggedInUser currentUser(HttpServletRequest req) {
        return (LoggedInUser) req.getSession().getAttribute(AppConstants.SESSION_USER);
    }

    private int parseCount(HttpServletRequest req) {
        return InputParser.parseLong(req.getParameter("count")).map(Long::intValue).orElse(-1);
    }

    private String returnSuffix(HttpServletRequest req) {
        String status = req.getParameter("returnStatus");
        boolean validStatus = status != null
                && Arrays.stream(DeviceStatus.values()).anyMatch(s -> s.name().equals(status));
        String street = InputParser.parseLong(req.getParameter("returnStreet")).map(String::valueOf).orElse("");
        long page = InputParser.parseLong(req.getParameter("returnPage")).orElse(1L);
        return "?status=" + (validStatus ? status : "") + "&street=" + street + "&page=" + Math.max(1, page) + "#lights";
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String flashKey, String message,
                          String suffix) throws IOException {
        FlashMessages.set(req, flashKey, message);
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_STREETLIGHTS + suffix);
    }
}