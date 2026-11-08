
package com.urbantech.smartcity360.controller;

import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.dto.PolicyForm;
import com.urbantech.smartcity360.dto.PolicyTestForm;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ResourceNotFoundException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.enums.PolicyCategory;
import com.urbantech.smartcity360.service.policy.PolicyManagementService;
import com.urbantech.smartcity360.service.policy.PolicyVariable;
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

@WebServlet(name = "CityPolicyServlet", urlPatterns = AppConstants.PATH_POLICIES)
public class CityPolicyServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CityPolicyServlet.class.getName());
    private static final String SYSTEM_ERROR = "System error. Please try again later.";

    private static final String ACTION_SAVE = "save";
    private static final String ACTION_TOGGLE = "toggle";
    private static final String ACTION_TEST = "test";
    private static final String ACTION_CHECK_ALL = "checkAll";

    private final PolicyManagementService policyService = new PolicyManagementService();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        FlashMessages.moveToRequest(req);
        loadRequestedForms(req);
        renderPage(req, resp);
    }

    private void loadRequestedForms(HttpServletRequest req) {
        if (req.getParameter("add") != null) {
            req.setAttribute(AppConstants.ATTR_OPEN_POLICY_MODAL, true);
        }
        InputParser.parseLong(req.getParameter("edit")).ifPresent(id -> {
            try {
                req.setAttribute(AppConstants.ATTR_POLICY_FORM, policyService.getForm(id));
                req.setAttribute(AppConstants.ATTR_OPEN_POLICY_MODAL, true);
            } catch (ResourceNotFoundException e) {
                req.setAttribute(AppConstants.FLASH_ERROR, e.getMessage());
            }
        });
        InputParser.parseLong(req.getParameter("test")).ifPresent(id -> {
            try {
                req.setAttribute(AppConstants.ATTR_TEST_FORM, policyService.newTestFormFor(id));
            } catch (ResourceNotFoundException e) {
                req.setAttribute(AppConstants.FLASH_ERROR, e.getMessage());
            }
        });
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action") == null ? "" : req.getParameter("action");

        switch (action) {
            case ACTION_SAVE -> save(req, resp);
            case ACTION_TOGGLE -> toggle(req, resp);
            case ACTION_TEST -> test(req, resp);
            case ACTION_CHECK_ALL -> checkAll(req, resp);
            default -> redirect(req, resp, AppConstants.FLASH_ERROR, "Unknown action.");
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PolicyForm form = new PolicyForm();
        form.setId(req.getParameter("id"));
        form.setName(req.getParameter("name"));
        form.setCategory(req.getParameter("category"));
        form.setRuleText(req.getParameter("ruleText"));

        try {
            policyService.save(form, currentUser(req));
            redirect(req, resp, AppConstants.FLASH_SUCCESS, "Policy saved.");
        } catch (ValidationException e) {
            showPolicyErrors(req, resp, form, e.getErrors());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Saving policy failed", e);
            showPolicyErrors(req, resp, form, List.of(SYSTEM_ERROR));
        }
    }

    private void toggle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean active = Boolean.parseBoolean(req.getParameter("active"));
        try {
            policyService.setActive(InputParser.parseLong(req.getParameter("policyId")).orElse(null), active);
            redirect(req, resp, AppConstants.FLASH_SUCCESS, active ? "Policy activated." : "Policy deactivated.");
        } catch (ResourceNotFoundException e) {
            redirect(req, resp, AppConstants.FLASH_ERROR, e.getMessage());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Changing policy status failed", e);
            redirect(req, resp, AppConstants.FLASH_ERROR, SYSTEM_ERROR);
        }
    }

    private void test(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PolicyTestForm form = readTestForm(req);
        req.setAttribute(AppConstants.ATTR_TEST_FORM, form);
        try {
            req.setAttribute(AppConstants.ATTR_TEST_RESULT, policyService.test(form));
        } catch (ValidationException e) {
            req.setAttribute(AppConstants.ATTR_TEST_ERRORS, e.getErrors());
        }
        renderPage(req, resp);
    }

    private void checkAll(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PolicyTestForm form = readTestForm(req);
        req.setAttribute(AppConstants.ATTR_TEST_FORM, form);
        try {
            req.setAttribute(AppConstants.ATTR_CHECK_ROWS, policyService.checkAll(form));
        } catch (ValidationException e) {
            req.setAttribute(AppConstants.ATTR_TEST_ERRORS, e.getErrors());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Checking policies failed", e);
            req.setAttribute(AppConstants.ATTR_TEST_ERRORS, List.of(SYSTEM_ERROR));
        }
        renderPage(req, resp);
    }


    private void renderPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "City Policies");
        req.setAttribute(AppConstants.ATTR_POLICY_ROWS, policyService.getRows());
        req.setAttribute(AppConstants.ATTR_POLICY_CATEGORIES, PolicyCategory.values());
        req.setAttribute(AppConstants.ATTR_POLICY_VARIABLES, PolicyVariable.values());
        setIfAbsent(req, AppConstants.ATTR_POLICY_FORM, new PolicyForm());
        setIfAbsent(req, AppConstants.ATTR_TEST_FORM, policyService.newTestForm(null));
        req.getRequestDispatcher(AppConstants.VIEW_POLICIES).forward(req, resp);
    }

    private void showPolicyErrors(HttpServletRequest req, HttpServletResponse resp, PolicyForm form,
                                  List<String> errors) throws ServletException, IOException {
        req.setAttribute(AppConstants.ATTR_POLICY_FORM, form);
        req.setAttribute(AppConstants.ATTR_POLICY_ERRORS, errors);
        req.setAttribute(AppConstants.ATTR_OPEN_POLICY_MODAL, true);
        renderPage(req, resp);
    }


    private PolicyTestForm readTestForm(HttpServletRequest req) {
        PolicyTestForm form = new PolicyTestForm();
        form.setRuleText(req.getParameter("ruleText"));
        for (PolicyVariable variable : PolicyVariable.values()) {
            form.getValues().put(variable.name(), req.getParameter("v_" + variable.name()));
        }
        form.setTime(req.getParameter("time"));
        return form;
    }

    private LoggedInUser currentUser(HttpServletRequest req) {
        return (LoggedInUser) req.getSession().getAttribute(AppConstants.SESSION_USER);
    }

    private void setIfAbsent(HttpServletRequest req, String name, Object value) {
        if (req.getAttribute(name) == null) {
            req.setAttribute(name, value);
        }
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String flashKey, String message)
            throws IOException {
        FlashMessages.set(req, flashKey, message);
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_POLICIES);
    }
}