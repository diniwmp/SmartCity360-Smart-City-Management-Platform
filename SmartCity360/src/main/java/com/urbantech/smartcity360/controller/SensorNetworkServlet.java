package com.urbantech.smartcity360.controller;

import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.dto.SensorForm;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ResourceNotFoundException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.enums.SensorProtocol;
import com.urbantech.smartcity360.model.enums.SensorType;
import com.urbantech.smartcity360.service.sensor.SensorNetworkService;
import com.urbantech.smartcity360.service.sensor.SimulationResult;
import com.urbantech.smartcity360.util.AppConstants;
import com.urbantech.smartcity360.util.FlashMessages;
import com.urbantech.smartcity360.util.InputParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "SensorNetworkServlet", urlPatterns = AppConstants.PATH_SENSORS)
public class SensorNetworkServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(SensorNetworkServlet.class.getName());
    private static final String SYSTEM_ERROR = "System error. Please try again later.";

    private static final String ACTION_SIMULATE_ALL = "simulateAll";
    private static final String ACTION_SIMULATE_ONE = "simulateOne";
    private static final String ACTION_ADD = "addSensor";
    private static final String ACTION_SET_PROTOCOL = "setProtocol";
    private static final Set<String> MANAGER_ACTIONS = Set.of(ACTION_ADD, ACTION_SET_PROTOCOL);

    private final SensorNetworkService sensorService = new SensorNetworkService();

    @FunctionalInterface
    private interface SensorAction {
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

        if (MANAGER_ACTIONS.contains(action) && !sensorService.canManage(currentUser(req).getRole())) {
            denyAccess(req, resp);
            return;
        }

        switch (action) {
            case ACTION_SIMULATE_ALL -> runAction(req, resp, "#simulation", () -> {
                SimulationResult result = sensorService.simulateAll();
                req.getSession().setAttribute(AppConstants.SESSION_SIMULATION, result);
                return "Readings sent from " + result.getLines().size() + " sensors.";
            });
            case ACTION_SIMULATE_ONE -> runAction(req, resp, "#simulation", () -> {
                SimulationResult result = sensorService.simulateSensor(
                        InputParser.parseLong(req.getParameter("sensorId")).orElse(null));
                req.getSession().setAttribute(AppConstants.SESSION_SIMULATION, result);
                return "Reading sent from " + result.getLines().get(0).getSensorCode() + ".";
            });
            case ACTION_SET_PROTOCOL -> runAction(req, resp, "#sensors", () -> {
                sensorService.changeProtocol(InputParser.parseLong(req.getParameter("sensorId")).orElse(null),
                        req.getParameter("protocol"));
                return "The sensor will now send its readings using the new protocol.";
            });
            case ACTION_ADD -> addSensor(req, resp);
            default -> redirect(req, resp, AppConstants.FLASH_ERROR, "Unknown action.", "");
        }
    }

    private void addSensor(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SensorForm form = new SensorForm();
        form.setSensorCode(req.getParameter("sensorCode"));
        form.setSensorType(req.getParameter("sensorType"));
        form.setProtocol(req.getParameter("protocol"));
        form.setZoneId(req.getParameter("zoneId"));

        try {
            sensorService.addSensor(form);
            redirect(req, resp, AppConstants.FLASH_SUCCESS, "Sensor added.", "#sensors");
        } catch (ValidationException e) {
            req.setAttribute(AppConstants.ATTR_SENSOR_FORM, form);
            req.setAttribute(AppConstants.ATTR_SENSOR_ERRORS, e.getErrors());
            renderPage(req, resp);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Adding sensor failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, "");
        }
    }

    private void runAction(HttpServletRequest req, HttpServletResponse resp, String suffix, SensorAction action)
            throws IOException {
        try {
            redirect(req, resp, AppConstants.FLASH_SUCCESS, action.perform(), suffix);
        } catch (ResourceNotFoundException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, e.getMessage(), suffix);
        } catch (ValidationException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, String.join(" ", e.getErrors()), suffix);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Sensor action failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, suffix);
        }
    }


    private void renderPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean canManage = sensorService.canManage(currentUser(req).getRole());
        Long filterSensor = InputParser.parseLong(req.getParameter("filter")).orElse(null);
        boolean onlyBreaches = req.getParameter("breaches") != null;

        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Sensor Network");
        req.setAttribute(AppConstants.ATTR_SENSOR_OVERVIEW, sensorService.getOverview());
        req.setAttribute(AppConstants.ATTR_SENSOR_ROWS, sensorService.getSensorRows());
        req.setAttribute(AppConstants.ATTR_SENSOR_READINGS, sensorService.getReadingRows(filterSensor, onlyBreaches));
        req.setAttribute(AppConstants.ATTR_FILTER_SENSOR, filterSensor);
        req.setAttribute(AppConstants.ATTR_ONLY_BREACHES, onlyBreaches);
        req.setAttribute(AppConstants.ATTR_SIMULATION, req.getSession().getAttribute(AppConstants.SESSION_SIMULATION));
        req.setAttribute(AppConstants.ATTR_SENSOR_PROTOCOLS, SensorProtocol.values());
        req.setAttribute(AppConstants.ATTR_CAN_MANAGE_SENSORS, canManage);
        if (canManage) {
            req.setAttribute(AppConstants.ATTR_SENSOR_TYPES, SensorType.values());
            req.setAttribute(AppConstants.ATTR_STREET_ZONES, sensorService.getStreetZones());
            req.setAttribute(AppConstants.ATTR_BUILDING_ZONES, sensorService.getBuildingZones());
            if (req.getAttribute(AppConstants.ATTR_SENSOR_FORM) == null) {
                req.setAttribute(AppConstants.ATTR_SENSOR_FORM, new SensorForm());
            }
        }
        req.getRequestDispatcher(AppConstants.VIEW_SENSORS).forward(req, resp);
    }

    private void denyAccess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Access Denied");
        req.getRequestDispatcher(AppConstants.VIEW_ACCESS_DENIED).forward(req, resp);
    }


    private LoggedInUser currentUser(HttpServletRequest req) {
        return (LoggedInUser) req.getSession().getAttribute(AppConstants.SESSION_USER);
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String flashKey, String message,
                          String suffix) throws IOException {
        FlashMessages.set(req, flashKey, message);
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_SENSORS + suffix);
    }
}