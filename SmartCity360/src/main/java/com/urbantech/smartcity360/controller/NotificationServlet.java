package com.urbantech.smartcity360.controller;
import com.urbantech.smartcity360.dto.AlertForm;
import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ResourceNotFoundException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.enums.AlertType;
import com.urbantech.smartcity360.model.enums.SeverityLevel;
import com.urbantech.smartcity360.service.notification.NotificationService;
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

@WebServlet(name = "NotificationServlet", urlPatterns = AppConstants.PATH_NOTIFICATIONS)
public class NotificationServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(NotificationServlet.class.getName());
    private static final String SYSTEM_ERROR = "System error. Please try again later.";

    private static final String ACTION_MARK_READ = "markRead";
    private static final String ACTION_MARK_ALL = "markAll";
    private static final String ACTION_CHECK_NOW = "checkNow";
    private static final String ACTION_SEND_TEST = "sendTest";
    private static final Set<String> MANAGER_ACTIONS = Set.of(ACTION_CHECK_NOW, ACTION_SEND_TEST);

    private final NotificationService notificationService = new NotificationService();

    @FunctionalInterface
    private interface NotificationAction {
        String perform() throws ResourceNotFoundException;
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
        String action = req.getParameter("action") == null ? "" : req.getParameter("action");

        if (MANAGER_ACTIONS.contains(action) && !notificationService.canManage(user.getRole())) {
            denyAccess(req, resp);
            return;
        }

        switch (action) {
            case ACTION_MARK_READ -> runAction(req, resp, () -> {
                notificationService.markRead(InputParser.parseLong(req.getParameter("notificationId")).orElse(null), user);
                return "";
            });
            case ACTION_MARK_ALL -> runAction(req, resp, () -> {
                int marked = notificationService.markAllRead(user);
                return marked + " notifications marked as read.";
            });
            case ACTION_CHECK_NOW -> runAction(req, resp, () -> notificationService.checkCity().toMessage());
            case ACTION_SEND_TEST -> sendTest(req, resp);
            default -> redirect(req, resp, AppConstants.FLASH_ERROR, "Unknown action.");
        }
    }

    private void sendTest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AlertForm form = new AlertForm();
        form.setAlertType(req.getParameter("alertType"));
        form.setSeverity(req.getParameter("severity"));
        form.setTitle(req.getParameter("title"));
        form.setMessage(req.getParameter("message"));

        try {
            int created = notificationService.sendTestAlert(form);
            redirect(req, resp, AppConstants.FLASH_SUCCESS, "Alert sent. " + created + " messages were created.");
        } catch (ValidationException e) {
            req.setAttribute(AppConstants.ATTR_ALERT_FORM, form);
            req.setAttribute(AppConstants.ATTR_ALERT_ERRORS, e.getErrors());
            renderPage(req, resp);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Sending alert failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR);
        }
    }

    private void runAction(HttpServletRequest req, HttpServletResponse resp, NotificationAction action)
            throws IOException {
        try {
            redirect(req, resp, AppConstants.FLASH_SUCCESS, action.perform());
        } catch (ResourceNotFoundException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, e.getMessage());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Notification action failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR);
        }
    }


    private void renderPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LoggedInUser user = currentUser(req);
        boolean canManage = notificationService.canManage(user.getRole());
        boolean unreadOnly = req.getParameter("unread") != null;

        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Notifications");
        req.setAttribute(AppConstants.ATTR_NOTIFICATION_OVERVIEW, notificationService.getOverview(user));
        req.setAttribute(AppConstants.ATTR_INBOX, notificationService.getInbox(user, unreadOnly));
        req.setAttribute(AppConstants.ATTR_UNREAD_ONLY, unreadOnly);
        req.setAttribute(AppConstants.ATTR_CAN_MANAGE_NOTIFICATIONS, canManage);
        if (canManage) {
            req.setAttribute(AppConstants.ATTR_DELIVERIES, notificationService.getDeliveries());
            req.setAttribute(AppConstants.ATTR_ALERT_TYPES, AlertType.values());
            req.setAttribute(AppConstants.ATTR_ALERT_SEVERITIES, SeverityLevel.values());
            if (req.getAttribute(AppConstants.ATTR_ALERT_FORM) == null) {
                req.setAttribute(AppConstants.ATTR_ALERT_FORM, new AlertForm());
            }
        }
        req.getRequestDispatcher(AppConstants.VIEW_NOTIFICATIONS).forward(req, resp);
    }

    private void denyAccess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Access Denied");
        req.getRequestDispatcher(AppConstants.VIEW_ACCESS_DENIED).forward(req, resp);
    }


    private LoggedInUser currentUser(HttpServletRequest req) {
        return (LoggedInUser) req.getSession().getAttribute(AppConstants.SESSION_USER);
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String flashKey, String message)
            throws IOException {
        if (message != null && !message.isEmpty()) {
            FlashMessages.set(req, flashKey, message);
        }
        String unread = "1".equals(req.getParameter("returnUnread")) ? "?unread=1" : "";
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_NOTIFICATIONS + unread);
    }
}