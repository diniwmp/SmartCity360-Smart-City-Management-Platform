package com.urbantech.smartcity360.controller;

import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.dto.ZoneForm;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ResourceNotFoundException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.service.zone.ZoneManagementService;
import com.urbantech.smartcity360.service.zone.ZoneTreeRow;
import com.urbantech.smartcity360.util.AppConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "ZoneHierarchyServlet", urlPatterns = AppConstants.PATH_ZONES)
public class ZoneHierarchyServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(ZoneHierarchyServlet.class.getName());
    private static final String SYSTEM_ERROR = "System error. Please try again later.";

    private static final String ACTION_SHUTDOWN = "shutdown";
    private static final String ACTION_RESTORE = "restore";
    private static final String ACTION_CREATE = "create";

    private final ZoneManagementService zoneService = new ZoneManagementService();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        moveFlashToRequest(req);
        showPage(req, resp, new ZoneForm(), null);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        if (!zoneService.canManage(currentUser(req).getRole())) {
            denyAccess(req, resp);
            return;
        }

        String action = req.getParameter("action") == null ? "" : req.getParameter("action");
        switch (action) {
            case ACTION_SHUTDOWN -> changePower(req, resp, true);
            case ACTION_RESTORE -> changePower(req, resp, false);
            case ACTION_CREATE -> createZone(req, resp);
            default -> redirectWithFlash(req, resp, AppConstants.FLASH_ERROR, "Unknown action.");
        }
    }

    private void changePower(HttpServletRequest req, HttpServletResponse resp, boolean shutdown) throws IOException {
        try {
            Long zoneId = parseId(req.getParameter("zoneId"))
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid zone selected."));

            int affected = shutdown ? zoneService.shutdownPower(zoneId) : zoneService.restorePower(zoneId);
            String verb = shutdown ? "Power shut down" : "Power restored";
            redirectWithFlash(req, resp, AppConstants.FLASH_SUCCESS, verb + " for " + affected + " zone(s).");
        } catch (ResourceNotFoundException e) {
            redirectWithFlash(req, resp, AppConstants.FLASH_ERROR, e.getMessage());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Power command failed", e);
            redirectWithFlash(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR);
        }
    }

    private void createZone(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ZoneForm form = readZoneForm(req);
        try {
            zoneService.createZone(form);
            redirectWithFlash(req, resp, AppConstants.FLASH_SUCCESS, "Zone created successfully.");
        } catch (ValidationException e) {
            showPage(req, resp, form, e.getErrors());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Zone creation failed", e);
            showPage(req, resp, form, List.of(SYSTEM_ERROR));
        }
    }


    private void showPage(HttpServletRequest req, HttpServletResponse resp, ZoneForm form, List<String> errors)
            throws ServletException, IOException {
        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Smart Zones");

        List<ZoneTreeRow> rows = zoneService.getTreeRows();
        req.setAttribute(AppConstants.ATTR_ZONE_ROWS, rows);

        req.setAttribute("zoneOverview", buildZoneOverview(rows));

        req.setAttribute(AppConstants.ATTR_CAN_MANAGE_ZONES, zoneService.canManage(currentUser(req).getRole()));
        req.setAttribute(AppConstants.ATTR_FORM, form);
        if (errors != null) {
            req.setAttribute(AppConstants.ATTR_ERRORS, errors);
        }
        loadReportIfRequested(req);
        req.getRequestDispatcher(AppConstants.VIEW_ZONES).forward(req, resp);
    }

    private Map<String, Object> buildZoneOverview(List<ZoneTreeRow> rows) {
        Map<String, Object> overview = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            overview.put("totalZones", 0);
            overview.put("buildings", 0);
            overview.put("usageKw", 0.0);
            overview.put("powerOffZones", 0);
            return overview;
        }

        int totalZones = rows.size();

        int buildings = rows.stream()
                .filter(r -> r.getDepth() == 0)
                .mapToInt(ZoneTreeRow::getBuildingCount)
                .sum();
        if (buildings == 0) {
            buildings = (int) rows.stream().filter(r -> !r.isHasChildren()).count();
        }

        double usageKw = rows.stream()
                .filter(r -> r.getDepth() == 0)
                .mapToDouble(ZoneTreeRow::getUsageKw)
                .sum();
        if (usageKw == 0.0) {
            usageKw = rows.stream()
                    .filter(r -> !r.isHasChildren())
                    .mapToDouble(ZoneTreeRow::getUsageKw)
                    .sum();
        }

        long powerOffZones = rows.stream()
                .filter(r -> (r.getPowerSummary() != null && "OFF".equalsIgnoreCase(r.getPowerSummary().name()))
                        || r.isCanRestore())
                .count();

        overview.put("totalZones", totalZones);
        overview.put("buildings", buildings);
        overview.put("usageKw", usageKw);
        overview.put("powerOffZones", powerOffZones);
        return overview;
    }

    private void loadReportIfRequested(HttpServletRequest req) {
        parseId(req.getParameter("report")).ifPresent(zoneId -> {
            try {
                req.setAttribute(AppConstants.ATTR_ZONE_REPORT, zoneService.reportUsage(zoneId));
            } catch (ResourceNotFoundException e) {
                req.setAttribute(AppConstants.FLASH_ERROR, e.getMessage());
            }
        });
    }

    private void denyAccess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Access Denied");
        req.getRequestDispatcher(AppConstants.VIEW_ACCESS_DENIED).forward(req, resp);
    }


    private ZoneForm readZoneForm(HttpServletRequest req) {
        ZoneForm form = new ZoneForm();
        form.setParentId(req.getParameter("parentId"));
        form.setName(req.getParameter("name"));
        form.setCode(req.getParameter("code"));
        form.setRatedLoadKw(req.getParameter("ratedLoadKw"));
        form.setMaintenanceCost(req.getParameter("maintenanceCost"));
        return form;
    }

    private LoggedInUser currentUser(HttpServletRequest req) {
        return (LoggedInUser) req.getSession().getAttribute(AppConstants.SESSION_USER);
    }

    private Optional<Long> parseId(String text) {
        try {
            return text == null ? Optional.empty() : Optional.of(Long.parseLong(text.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void redirectWithFlash(HttpServletRequest req, HttpServletResponse resp, String key, String message)
            throws IOException {
        req.getSession().setAttribute(key, message);
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_ZONES);
    }

    private void moveFlashToRequest(HttpServletRequest req) {
        HttpSession session = req.getSession();
        for (String key : new String[]{AppConstants.FLASH_SUCCESS, AppConstants.FLASH_ERROR}) {
            Object message = session.getAttribute(key);
            if (message != null) {
                req.setAttribute(key, message);
                session.removeAttribute(key);
            }
        }
    }
}