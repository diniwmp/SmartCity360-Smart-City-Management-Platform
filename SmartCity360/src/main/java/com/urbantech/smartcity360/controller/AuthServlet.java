package com.urbantech.smartcity360.controller;

import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.dto.RegistrationForm;
import com.urbantech.smartcity360.exception.AuthenticationException;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.enums.AppModule;
import com.urbantech.smartcity360.service.auth.AuthService;
import com.urbantech.smartcity360.util.AppConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Login / Logout / Register (MVC Controller). Business logic AuthService එකේ. */
@WebServlet(name = "AuthServlet",
        urlPatterns = {AppConstants.PATH_LOGIN, AppConstants.PATH_LOGOUT, AppConstants.PATH_REGISTER})
public class AuthServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(AuthServlet.class.getName());
    private static final String SYSTEM_ERROR = "System error. Please try again later.";

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getServletPath()) {
            case AppConstants.PATH_LOGOUT -> logout(req, resp);
            case AppConstants.PATH_REGISTER -> req.getRequestDispatcher(AppConstants.VIEW_REGISTER).forward(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + AppConstants.PATH_LOGIN_PAGE);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (AppConstants.PATH_REGISTER.equals(req.getServletPath())) {
            register(req, resp);
        } else {
            login(req, resp);
        }
    }

    // ------------------------------------------------------------------ login

    private void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            LoggedInUser user = authService.login(username, password);
            startSession(req, user);
            resp.sendRedirect(req.getContextPath() + AppConstants.PATH_DASHBOARD);
        } catch (AuthenticationException e) {
            showLoginError(req, resp, username, e.getMessage());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Login failed due to a database error", e);
            showLoginError(req, resp, username, SYSTEM_ERROR);
        }
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_LOGIN_PAGE + "?logout=1");
    }

    /** Session fixation වළක්වන්න පරණ session එක අවලංගු කර අලුත් එකක් හදයි. */
    private void startSession(HttpServletRequest req, LoggedInUser user) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(AppConstants.SESSION_TIMEOUT_SECONDS);
        session.setAttribute(AppConstants.SESSION_USER, user);
        session.setAttribute(AppConstants.SESSION_MENU, AppModule.visibleFor(user.getRole()));
    }

    private void showLoginError(HttpServletRequest req, HttpServletResponse resp, String username, String message)
            throws ServletException, IOException {
        req.setAttribute(AppConstants.ATTR_ERROR, message);
        req.setAttribute(AppConstants.ATTR_ENTERED_USERNAME, username);
        req.getRequestDispatcher(AppConstants.PATH_LOGIN_PAGE).forward(req, resp);
    }

    // --------------------------------------------------------------- register

    private void register(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RegistrationForm form = readRegistrationForm(req);

        try {
            authService.registerResident(form);
            resp.sendRedirect(req.getContextPath() + AppConstants.PATH_LOGIN_PAGE + "?registered=1");
        } catch (ValidationException e) {
            showRegisterErrors(req, resp, form, e.getErrors());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Registration failed due to a database error", e);
            showRegisterErrors(req, resp, form, List.of(SYSTEM_ERROR));
        }
    }

    private RegistrationForm readRegistrationForm(HttpServletRequest req) {
        RegistrationForm form = new RegistrationForm();
        form.setFullName(req.getParameter("fullName"));
        form.setUsername(req.getParameter("username"));
        form.setEmail(req.getParameter("email"));
        form.setPhone(req.getParameter("phone"));
        form.setPassword(req.getParameter("password"));
        form.setConfirmPassword(req.getParameter("confirmPassword"));
        return form;
    }

    private void showRegisterErrors(HttpServletRequest req, HttpServletResponse resp,
                                    RegistrationForm form, List<String> errors) throws ServletException, IOException {
        req.setAttribute(AppConstants.ATTR_FORM, form);
        req.setAttribute(AppConstants.ATTR_ERRORS, errors);
        req.getRequestDispatcher(AppConstants.VIEW_REGISTER).forward(req, resp);
    }
}