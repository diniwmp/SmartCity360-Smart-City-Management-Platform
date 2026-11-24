package com.urbantech.smartcity360.controller;

import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.exception.ReportExportException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.service.report.*;
import com.urbantech.smartcity360.util.AppConstants;
import com.urbantech.smartcity360.util.FlashMessages;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebServlet(name = "ReportServlet", urlPatterns = {AppConstants.PATH_REPORTS, AppConstants.PATH_REPORT_DOWNLOAD})
public class ReportServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(ReportServlet.class.getName());
    private static final String SYSTEM_ERROR = "The report could not be created. Please try again later.";

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (AppConstants.PATH_REPORT_DOWNLOAD.equals(req.getServletPath())) {
            download(req, resp);
        } else {
            FlashMessages.moveToRequest(req);
            showPage(req, resp);
        }
    }


    private void showPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Set<ReportSectionType> selected = selectedSections(req);

        req.setAttribute(AppConstants.ATTR_PAGE_TITLE, "Reports");
        req.setAttribute(AppConstants.ATTR_REPORT_TYPES, ReportSectionType.values());
        req.setAttribute(AppConstants.ATTR_SELECTED_SECTIONS, selected.stream().map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        req.setAttribute(AppConstants.ATTR_REPORT_TITLE, reportService.cleanTitle(req.getParameter("title")));

        try {
            req.setAttribute(AppConstants.ATTR_REPORT, reportService.build(selected, req.getParameter("title"), currentUser(req)));
        } catch (ValidationException e) {
            req.setAttribute(AppConstants.ATTR_REPORT_ERROR, String.join(" ", e.getErrors()));
        }
        req.getRequestDispatcher(AppConstants.VIEW_REPORTS).forward(req, resp);
    }


    private void download(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ReportFormat format = ReportFormat.fromParam(req.getParameter("format")).orElse(null);
        if (format == null) {
            failBackToPage(req, resp, "Choose PDF or CSV.");
            return;
        }

        try {
            CityReport report = reportService.build(selectedSections(req), req.getParameter("title"), currentUser(req));
            ExportedReport file = reportService.export(report, format);

            resp.setContentType(file.getContentType());
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + file.getFileName() + "\"");
            resp.setHeader("Cache-Control", "no-store");
            resp.setContentLength(file.getContent().length);
            resp.getOutputStream().write(file.getContent());
        } catch (ValidationException e) {
            failBackToPage(req, resp, String.join(" ", e.getErrors()));
        } catch (DataAccessException | ReportExportException e) {
            LOG.log(Level.SEVERE, "Report download failed", e);
            failBackToPage(req, resp, SYSTEM_ERROR);
        }
    }


    private Set<ReportSectionType> selectedSections(HttpServletRequest req) {
        return reportService.parseSections(req.getParameterValues("sections"), req.getParameter("submitted") != null);
    }

    private LoggedInUser currentUser(HttpServletRequest req) {
        return (LoggedInUser) req.getSession().getAttribute(AppConstants.SESSION_USER);
    }

    private void failBackToPage(HttpServletRequest req, HttpServletResponse resp, String message) throws IOException {
        FlashMessages.set(req, AppConstants.FLASH_ERROR, message);
        resp.sendRedirect(req.getContextPath() + AppConstants.PATH_REPORTS);
    }
}