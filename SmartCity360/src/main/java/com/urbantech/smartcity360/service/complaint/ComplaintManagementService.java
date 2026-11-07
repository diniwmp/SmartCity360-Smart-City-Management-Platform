package com.urbantech.smartcity360.service.complaint;

import com.urbantech.smartcity360.dao.ComplaintDAO;
import com.urbantech.smartcity360.dao.UserDAO;
import com.urbantech.smartcity360.dao.ZoneDAO;
import com.urbantech.smartcity360.dto.ComplaintForm;
import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.exception.ActionNotAllowedException;
import com.urbantech.smartcity360.exception.ResourceNotFoundException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.ComplaintEntity;
import com.urbantech.smartcity360.model.User;
import com.urbantech.smartcity360.model.ZoneEntity;
import com.urbantech.smartcity360.model.enums.*;
import com.urbantech.smartcity360.util.AppConstants;
import com.urbantech.smartcity360.util.InputParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ComplaintManagementService {

    private final ComplaintDAO complaintDAO;
    private final UserDAO userDAO;
    private final ZoneDAO zoneDAO;
    private final ComplaintDispatchService dispatchService;
    private final ComplaintAccessPolicy policy;

    public ComplaintManagementService() {
        this(new ComplaintDAO(), new UserDAO(), new ZoneDAO(), new ComplaintDispatchService(), new ComplaintAccessPolicy());
    }

    public ComplaintManagementService(ComplaintDAO complaintDAO, UserDAO userDAO, ZoneDAO zoneDAO,
                                      ComplaintDispatchService dispatchService, ComplaintAccessPolicy policy) {
        this.complaintDAO = complaintDAO;
        this.userDAO = userDAO;
        this.zoneDAO = zoneDAO;
        this.dispatchService = dispatchService;
        this.policy = policy;
    }

    public boolean canSubmit(UserRole role) {
        return role == UserRole.RESIDENT;
    }


    public List<ComplaintRow> getRows(LoggedInUser user) {
        return loadVisible(user).stream()
                .map(complaint -> ComplaintRow.of(complaint, policy, user))
                .toList();
    }

    public static List<ComplaintRow> filter(List<ComplaintRow> rows, String filter) {
        if (filter == null || filter.isBlank()) {
            return rows;
        }
        if (AppConstants.COMPLAINT_FILTER_OPEN.equals(filter)) {
            return rows.stream().filter(ComplaintRow::isOpen).toList();
        }
        EscalationStatus status = parseEnum(EscalationStatus.class, filter);
        return status == null ? rows : rows.stream().filter(row -> row.getStatus() == status).toList();
    }

    public ComplaintDetail getDetail(Long id, LoggedInUser user) throws ResourceNotFoundException {
        ComplaintEntity complaint = loadViewable(id, user);
        List<TimelineEntry> timeline = complaintDAO.findHistory(id).stream().map(TimelineEntry::of).toList();
        return ComplaintDetail.of(ComplaintRow.of(complaint, policy, user), complaint.getDescription(), timeline);
    }

    public List<SeverityOption> getSeverityOptions() {
        return Arrays.stream(SeverityLevel.values())
                .map(severity -> new SeverityOption(severity, dispatchService.previewLevel(severity)))
                .toList();
    }

    public List<ZoneEntity> getStreetZones() {
        return zoneDAO.findByType(ZoneType.STREET);
    }

    public List<ZoneEntity> getBuildingZones() {
        return zoneDAO.findByType(ZoneType.BUILDING);
    }


    public ComplaintEntity submit(ComplaintForm form, LoggedInUser user) throws ValidationException {
        List<String> errors = new ArrayList<>();

        String title = InputParser.clean(form.getTitle());
        String description = InputParser.clean(form.getDescription());
        if (title.length() < AppConstants.COMPLAINT_TITLE_MIN || title.length() > AppConstants.COMPLAINT_TITLE_MAX) {
            errors.add(String.format("Title must be %d-%d characters.",
                    AppConstants.COMPLAINT_TITLE_MIN, AppConstants.COMPLAINT_TITLE_MAX));
        }
        if (description.length() < AppConstants.COMPLAINT_DESCRIPTION_MIN
                || description.length() > AppConstants.COMPLAINT_DESCRIPTION_MAX) {
            errors.add(String.format("Description must be %d-%d characters.",
                    AppConstants.COMPLAINT_DESCRIPTION_MIN, AppConstants.COMPLAINT_DESCRIPTION_MAX));
        }

        ComplaintCategory category = parseEnum(ComplaintCategory.class, form.getCategory());
        if (category == null) {
            errors.add("Select a category.");
        }
        SeverityLevel severity = parseEnum(SeverityLevel.class, form.getSeverity());
        if (severity == null) {
            errors.add("Select how severe the problem is.");
        }

        ZoneEntity zone = null;
        if (!InputParser.clean(form.getZoneId()).isEmpty()) {
            zone = InputParser.parseLong(form.getZoneId()).flatMap(zoneDAO::findById).orElse(null);
            if (zone == null) {
                errors.add("The selected location was not found.");
            }
        }

        User citizen = userDAO.findById(user.getId()).orElse(null);
        if (citizen == null) {
            errors.add("Your account was not found.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        ComplaintEntity complaint = new ComplaintEntity();
        complaint.setReferenceNo(newReferenceNo());
        complaint.setCitizen(citizen);
        complaint.setZone(zone);
        complaint.setTitle(title);
        complaint.setDescription(description);
        complaint.setCategory(category);
        complaint.setSeverity(severity);
        complaint.setStatus(EscalationStatus.SUBMITTED);
        complaint.setCurrentLevel(EscalationLevel.LOCAL_STAFF);

        EscalationRequest request = new EscalationRequest(complaint, citizen);
        request.recordSubmission();
        dispatchService.route(request);
        assignResponder(complaint);

        return complaintDAO.saveWithHistory(complaint, request.getTrail());
    }


    public void startWork(Long id, LoggedInUser user) throws ResourceNotFoundException, ActionNotAllowedException {
        ComplaintEntity complaint = loadActionable(id, user);
        if (complaint.getStatus() != EscalationStatus.SUBMITTED && complaint.getStatus() != EscalationStatus.ESCALATED) {
            throw new ActionNotAllowedException("Work can only be started on a new or escalated complaint.");
        }
        User actor = actorOf(user);
        complaint.setStatus(EscalationStatus.IN_PROGRESS);
        if (actor != null && complaint.getCurrentLevel().getResponderRole() == actor.getRole()) {
            complaint.setAssignedTo(actor);
        }
        touch(complaint);
        complaintDAO.updateWithHistory(complaint, List.of(HistoryEntries.create(HistoryEvent.WORK_STARTED,
                complaint.getCurrentLevel(), complaint.getCurrentLevel(), "Work started by " + user.getFullName() + ".", actor)));
    }

    public void resolve(Long id, LoggedInUser user, String note)
            throws ResourceNotFoundException, ActionNotAllowedException, ValidationException {
        ComplaintEntity complaint = loadActionable(id, user);
        requireOpen(complaint);
        String resolution = requireNote(note, "Please describe how the problem was resolved");

        complaint.setStatus(EscalationStatus.RESOLVED);
        complaint.setResolvedAt(LocalDateTime.now());
        touch(complaint);
        complaintDAO.updateWithHistory(complaint, List.of(HistoryEntries.create(HistoryEvent.RESOLVED,
                complaint.getCurrentLevel(), complaint.getCurrentLevel(), resolution, actorOf(user))));
    }

    public EscalationLevel escalate(Long id, LoggedInUser user, String reason)
            throws ResourceNotFoundException, ActionNotAllowedException, ValidationException {
        ComplaintEntity complaint = loadActionable(id, user);
        requireOpen(complaint);
        EscalationLevel current = complaint.getCurrentLevel();
        EscalationLevel next = current.next()
                .orElseThrow(() -> new ActionNotAllowedException("This complaint is already at the highest level."));
        String why = requireNote(reason, "Please give a reason for escalating");

        EscalationRequest request = new EscalationRequest(complaint, actorOf(user));
        request.recordManualEscalation(current, next, why);
        dispatchService.routeFrom(next, request);
        assignResponder(complaint);
        touch(complaint);
        complaintDAO.updateWithHistory(complaint, request.getTrail());
        return complaint.getCurrentLevel();
    }

    public void close(Long id, LoggedInUser user) throws ResourceNotFoundException, ActionNotAllowedException {
        ComplaintEntity complaint = loadViewable(id, user);
        if (!policy.canClose(user, complaint)) {
            throw new ActionNotAllowedException("Only the resident who reported it can close a resolved complaint.");
        }
        complaint.setStatus(EscalationStatus.CLOSED);
        touch(complaint);
        complaintDAO.updateWithHistory(complaint, List.of(HistoryEntries.create(HistoryEvent.CLOSED,
                complaint.getCurrentLevel(), complaint.getCurrentLevel(), "Resident confirmed the issue is fixed.", actorOf(user))));
    }



    private List<ComplaintEntity> loadVisible(LoggedInUser user) {
        return switch (user.getRole()) {
            case RESIDENT -> complaintDAO.findByCitizen(user.getId());
            case ADMIN, CITY_MANAGER -> complaintDAO.findAllWithDetails();
            case SUPERVISOR -> complaintDAO.findByLevels(EnumSet.of(EscalationLevel.LOCAL_STAFF, EscalationLevel.SUPERVISOR));
            case FIELD_STAFF -> complaintDAO.findByLevels(EnumSet.of(EscalationLevel.LOCAL_STAFF));
        };
    }

    private ComplaintEntity loadViewable(Long id, LoggedInUser user) throws ResourceNotFoundException {
        return complaintDAO.findByIdWithDetails(id)
                .filter(complaint -> policy.canView(user, complaint))
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found."));
    }

    private ComplaintEntity loadActionable(Long id, LoggedInUser user)
            throws ResourceNotFoundException, ActionNotAllowedException {
        ComplaintEntity complaint = loadViewable(id, user);
        if (!policy.canAct(user, complaint)) {
            throw new ActionNotAllowedException("This complaint is currently handled at the "
                    + complaint.getCurrentLevel().getLabel() + " level, so you cannot act on it.");
        }
        return complaint;
    }

    private void requireOpen(ComplaintEntity complaint) throws ActionNotAllowedException {
        if (!complaint.getStatus().isOpen()) {
            throw new ActionNotAllowedException("This complaint is already " + complaint.getStatus().getLabel().toLowerCase() + ".");
        }
    }

    private String requireNote(String text, String message) throws ValidationException {
        String note = InputParser.clean(text);
        if (note.length() < AppConstants.COMPLAINT_NOTE_MIN) {
            throw new ValidationException(List.of(message + " (at least " + AppConstants.COMPLAINT_NOTE_MIN + " characters)."));
        }
        return note;
    }

    private void assignResponder(ComplaintEntity complaint) {
        complaint.setAssignedTo(userDAO.findFirstActiveByRole(complaint.getCurrentLevel().getResponderRole()).orElse(null));
    }

    private User actorOf(LoggedInUser user) {
        return userDAO.findById(user.getId()).orElse(null);
    }

    private void touch(ComplaintEntity complaint) {
        complaint.setUpdatedAt(LocalDateTime.now());
    }

    private String newReferenceNo() {
        String reference;
        do {
            reference = AppConstants.COMPLAINT_REFERENCE_PREFIX + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        } while (complaintDAO.existsByReferenceNo(reference));
        return reference;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}