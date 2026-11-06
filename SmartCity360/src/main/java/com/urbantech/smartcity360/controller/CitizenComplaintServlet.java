package com.urbantech.smartcity360.controller;
// Chain of Responsibility logic service layer
import com.urbantech.smartcity360.dto.ComplaintForm;
import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.exception.ActionNotAllowedException;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ResourceNotFoundException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.ComplaintEntity;
import com.urbantech.smartcity360.model.enums.ComplaintCategory;
import com.urbantech.smartcity360.model.enums.EscalationLevel;
import com.urbantech.smartcity360.model.enums.EscalationStatus;
import com.urbantech.smartcity360.service.complaint.ComplaintManagementService;
import com.urbantech.smartcity360.service.complaint.ComplaintRow;
import com.urbantech.smartcity360.service.complaint.ComplaintStats;
import com.urbantech.smartcity360.util.AppConstants;
import com.urbantech.smartcity360.util.FlashMessages;
import com.urbantech.smartcity360.util.InputParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "CitizenComplaintServlet", urlPatterns = AppConstants.PATH_COMPLAINTS)
public class CitizenComplaintServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CitizenComplaintServlet.class.getName());
    private static final String SYSTEM_ERROR = "System error. Please try again later.";

    private static final String ACTION_SUBMIT = "submit";
    private static final String ACTION_START = "start";
    private static final String ACTION_RESOLVE = "resolve";
    private static final String ACTION_ESCALATE = "escalate";
    private static final String ACTION_CLOSE = "close";

    private final ComplaintManagementService complaintService = new ComplaintManagementService();

    @FunctionalInterface
    private interface ComplaintAction {
        String perform() throws ResourceNotFoundException, ActionNotAllowedException, ValidationException;
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        FlashMessages.moveToRequest(req);
        renderPage(req, resp);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        LoggedInUser user = currentUser(req);
        Long id = InputParser.parseLong(req.getParameter("complaintId")).orElse(null);
        String action = req.getParameter("action") == null ? "" : req.getParameter("action");

        switch (action) {
            case ACTION_SUBMIT -> submit(req, resp, user);
            case ACTION_START -> runAction(req, resp, id, () -> {
                complaintService.startWork(id, user);
                return "Work started on the complaint.";
            });
            case ACTION_RESOLVE -> runAction(req, resp, id, () -> {
                complaintService.resolve(id, user, req.getParameter("note"));
                return "Complaint marked as resolved.";
            });
            case ACTION_ESCALATE -> runAction(req, resp, id, () -> {
                EscalationLevel level = complaintService.escalate(id, user, req.getParameter("reason"));
                return "Complaint escalated to " + level.getLabel() + ".";
            });
            case ACTION_CLOSE -> runAction(req, resp, id, () -> {
                complaintService.close(id, user);
                return "Complaint closed. Thank you for confirming.";
            });
            default -> redirect(req, resp, AppConstants.FLASH_ERROR, "Unknown action.", "");
        }
    }

    private void submit(HttpServletRequest req, HttpServletResponse resp, LoggedInUser user)
            throws ServletException, IOException {
        if (!complaintService.canSubmit(user.getRole())) {
            denyAccess(req, resp);
            return;
        }
        ComplaintForm form = new ComplaintForm();
        form.setTitle(req.getParameter("title"));
        form.setDescription(req.getParameter("description"));
        form.setCategory(req.getParameter("category"));
        form.setSeverity(req.getParameter("severity"));
        form.setZoneId(req.getParameter("zoneId"));

        try {
            ComplaintEntity complaint = complaintService.submit(form, user);
            redirect(req, resp, AppConstants.FLASH_SUCCESS,
                    "Complaint " + complaint.getReferenceNo() + " submitted. It is now with: "
                            + complaint.getCurrentLevel().getLabel() + ".",
                    "?view=" + complaint.getId() + "#detail");
        } catch (ValidationException e) {
            req.setAttribute(AppConstants.ATTR_COMPLAINT_FORM, form);
            req.setAttribute(AppConstants.ATTR_COMPLAINT_ERRORS, e.getErrors());
            renderPage(req, resp);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Submitting complaint failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, "");
        }
    }

    private void runAction(HttpServletRequest req, HttpServletResponse resp, Long id, ComplaintAction action)
            throws IOException {
        String suffix = id == null ? "" : "?view=" + id + "#detail";
        if (id == null) {
            redirect(req, resp, AppConstants.FLASH_ERROR, "Invalid complaint.", "");
            return;
        }
        try {
            redirect(req, resp, AppConstants.FLASH_SUCCESS, action.perform(), suffix);
        } catch (ResourceNotFoundException | ActionNotAllowedException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, e.getMessage(), suffix);
        } catch (ValidationException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, String.join(" ", e.getErrors()), suffix);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Complaint action failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR, suffix);
        }
    }


    private void renderPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LoggedInUser user = currentUser(req);
        String filter = req.getParameter("status") == null ? "" : req.getParameter("status");
        boolean canSubmit = complaintService.canSubmit(user.getRole());

        List<ComplaintRow> allRows = complaintService.getRows(user);
        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Complaints");
        req.setAttribute(AppConstants.ATTR_COMPLAINT_ROWS, ComplaintManagementService.filter(allRows, filter));
        req.setAttribute(AppConstants.ATTR_COMPLAINT_STATS, ComplaintStats.of(allRows));
        req.setAttribute(AppConstants.ATTR_STATUS_FILTER, filter);
        req.setAttribute(AppConstants.ATTR_COMPLAINT_STATUSES, EscalationStatus.values());
        req.setAttribute(AppConstants.ATTR_ESCALATION_LEVELS, EscalationLevel.values());
        req.setAttribute(AppConstants.ATTR_CAN_SUBMIT, canSubmit);

        if (canSubmit) {
            req.setAttribute(AppConstants.ATTR_SEVERITY_OPTIONS, complaintService.getSeverityOptions());
            req.setAttribute(AppConstants.ATTR_CATEGORIES, ComplaintCategory.values());
            req.setAttribute(AppConstants.ATTR_STREET_ZONES, complaintService.getStreetZones());
            req.setAttribute(AppConstants.ATTR_BUILDING_ZONES, complaintService.getBuildingZones());
        }
        loadDetail(req, user);
        if (req.getAttribute(AppConstants.ATTR_COMPLAINT_FORM) == null) {
            req.setAttribute(AppConstants.ATTR_COMPLAINT_FORM, new ComplaintForm());
        }
        req.getRequestDispatcher(AppConstants.VIEW_COMPLAINTS).forward(req, resp);
    }

    private void loadDetail(HttpServletRequest req, LoggedInUser user) {
        InputParser.parseLong(req.getParameter("view")).ifPresent(id -> {
            try {
                req.setAttribute(AppConstants.ATTR_COMPLAINT_DETAIL, complaintService.getDetail(id, user));
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

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String flashKey, String message,
                          String suffix) throws IOException {
        FlashMessages.set(req, flashKey, message);
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_COMPLAINTS + suffix);
    }
}