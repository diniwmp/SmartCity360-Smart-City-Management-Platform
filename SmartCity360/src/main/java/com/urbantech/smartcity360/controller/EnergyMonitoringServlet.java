package com.urbantech.smartcity360.controller;

import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.dto.MeterForm;
import com.urbantech.smartcity360.dto.SendReadingForm;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ResourceNotFoundException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.enums.SecurityLayer;
import com.urbantech.smartcity360.service.energy.EnergyMonitoringService;
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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@WebServlet(name = "EnergyMonitoringServlet", urlPatterns = AppConstants.PATH_ENERGY)
public class EnergyMonitoringServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(EnergyMonitoringServlet.class.getName());
    private static final String SYSTEM_ERROR = "System error. Please try again later.";

    private static final String ACTION_SEND = "send";
    private static final String ACTION_SIMULATE = "simulate";
    private static final String ACTION_ADD_METER = "addMeter";
    private static final String ACTION_TAMPER = "tamper";

    private final EnergyMonitoringService energyService = new EnergyMonitoringService();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        FlashMessages.moveToRequest(req);
        renderPage(req, resp);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action") == null ? "" : req.getParameter("action");

        switch (action) {
            case ACTION_SEND -> sendReading(req, resp);
            case ACTION_SIMULATE -> simulate(req, resp);
            case ACTION_ADD_METER -> addMeter(req, resp);
            case ACTION_TAMPER -> tamper(req, resp);
            default -> redirect(req, resp, AppConstants.FLASH_ERROR, "Unknown action.", "");
        }
    }

    private void sendReading(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SendReadingForm form = new SendReadingForm();
        form.setMeterId(req.getParameter("meterId"));
        form.setKwh(req.getParameter("kwh"));
        form.setVoltage(req.getParameter("voltage"));
        form.setLayers(req.getParameterValues("layers"));

        try {
            Long readingId = energyService.sendReading(form);
            redirect(req, resp, AppConstants.FLASH_SUCCESS,
                    "Reading #" + readingId + " was secured and stored. Inspect it below.",
                    "?inspect=" + readingId + "#inspect");
        } catch (ValidationException e) {
            req.setAttribute(AppConstants.ATTR_SEND_FORM, form);
            req.setAttribute(AppConstants.ATTR_SEND_ERRORS, e.getErrors());
            req.setAttribute(AppConstants.ATTR_SELECTED_LAYERS, layerNames(form.getLayers()));
            renderPage(req, resp);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Sending reading failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, "");
        }
    }

    private void simulate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int count = energyService.simulate(req.getParameterValues("layers"));
            redirect(req, resp, AppConstants.FLASH_SUCCESS, "Simulated " + count + " readings (one per active meter).", "");
        } catch (ValidationException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, String.join(" ", e.getErrors()), "");
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Simulation failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, "");
        }
    }

    private void addMeter(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!energyService.canManageMeters(currentUser(req).getRole())) {
            denyAccess(req, resp);
            return;
        }
        MeterForm form = new MeterForm();
        form.setZoneId(req.getParameter("zoneId"));
        form.setMeterCode(req.getParameter("meterCode"));

        try {
            energyService.addMeter(form);
            redirect(req, resp, AppConstants.FLASH_SUCCESS, "Meter added successfully.", "");
        } catch (ValidationException e) {
            req.setAttribute(AppConstants.ATTR_METER_FORM, form);
            req.setAttribute(AppConstants.ATTR_METER_ERRORS, e.getErrors());
            renderPage(req, resp);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Adding meter failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, "");
        }
    }

    private void tamper(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!energyService.canManageMeters(currentUser(req).getRole())) {
            denyAccess(req, resp);
            return;
        }
        try {
            Long readingId = InputParser.parseLong(req.getParameter("readingId"))
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid reading."));
            energyService.tamperReading(readingId);
            redirect(req, resp, AppConstants.FLASH_ERROR,
                    "Demo: the stored payload of reading #" + readingId + " was altered. Inspect it to see what the receiver detects.",
                    "?inspect=" + readingId + "#inspect");
        } catch (ResourceNotFoundException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, e.getMessage(), "");
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Tamper demo failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, "");
        }
    }


    private void renderPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LoggedInUser user = currentUser(req);
        Long filterMeter = InputParser.parseLong(req.getParameter("filter")).orElse(null);
        boolean canManage = energyService.canManageMeters(user.getRole());

        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Energy Monitoring");
        req.setAttribute(AppConstants.ATTR_ENERGY_OVERVIEW, energyService.getOverview());
        req.setAttribute(AppConstants.ATTR_METER_ROWS, energyService.getMeterRows());
        req.setAttribute(AppConstants.ATTR_READING_ROWS, energyService.getRecentReadings(filterMeter));
        req.setAttribute(AppConstants.ATTR_FILTER_METER, filterMeter);
        req.setAttribute(AppConstants.ATTR_SECURITY_LAYERS, SecurityLayer.values());
        req.setAttribute(AppConstants.ATTR_CAN_MANAGE_METERS, canManage);
        if (canManage) {
            req.setAttribute(AppConstants.ATTR_BUILDING_ZONES, energyService.getBuildingZones());
        }
        loadInspection(req);

        setIfAbsent(req, AppConstants.ATTR_SEND_FORM, new SendReadingForm());
        setIfAbsent(req, AppConstants.ATTR_METER_FORM, new MeterForm());
        setIfAbsent(req, AppConstants.ATTR_SELECTED_LAYERS, allLayerNames());

        req.getRequestDispatcher(AppConstants.VIEW_ENERGY).forward(req, resp);
    }

    private void loadInspection(HttpServletRequest req) {
        InputParser.parseLong(req.getParameter("inspect")).ifPresent(readingId -> {
            try {
                req.setAttribute(AppConstants.ATTR_INSPECTION, energyService.inspect(readingId));
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


    private LoggedInUser currentUser(HttpServletRequest req) {
        return (LoggedInUser) req.getSession().getAttribute(AppConstants.SESSION_USER);
    }

    private void setIfAbsent(HttpServletRequest req, String name, Object value) {
        if (req.getAttribute(name) == null) {
            req.setAttribute(name, value);
        }
    }

    private Set<String> layerNames(String[] values) {
        return values == null ? Set.of() : new LinkedHashSet<>(Arrays.asList(values));
    }

    private Set<String> allLayerNames() {
        return Arrays.stream(SecurityLayer.values()).map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String flashKey, String message,
                          String suffix) throws IOException {
        FlashMessages.set(req, flashKey, message);
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_ENERGY + suffix);
    }
}