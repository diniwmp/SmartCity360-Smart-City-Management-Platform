package com.urbantech.smartcity360.service.auth;

import com.urbantech.smartcity360.dao.UserDAO;
import com.urbantech.smartcity360.dto.LoggedInUser;
import com.urbantech.smartcity360.dto.RegistrationForm;
import com.urbantech.smartcity360.exception.AuthenticationException;
import com.urbantech.smartcity360.exception.ValidationException;
import com.urbantech.smartcity360.model.User;
import com.urbantech.smartcity360.model.enums.UserRole;
import com.urbantech.smartcity360.util.AppConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password.";

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{9,15}$");

    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }


    public LoggedInUser login(String username, String password) throws AuthenticationException {
        if (isBlank(username) || isBlank(password)) {
            throw new AuthenticationException("Please enter both username and password.");
        }

        User user = userDAO.findByUsername(username.trim())
                .filter(User::isActive)
                .filter(u -> passwordMatches(password, u.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));

        return LoggedInUser.from(user);
    }


    public void registerResident(RegistrationForm form) throws ValidationException {
        List<String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        userDAO.save(toResident(form));
    }

    private List<String> validate(RegistrationForm form) {
        List<String> errors = new ArrayList<>();
        validateFullName(form, errors);
        validateUsername(form, errors);
        validateEmail(form, errors);
        validatePhone(form, errors);
        validatePassword(form, errors);
        return errors;
    }

    private void validateFullName(RegistrationForm form, List<String> errors) {
        String name = clean(form.getFullName());
        if (name.length() < 2 || name.length() > AppConstants.MAX_NAME_LENGTH) {
            errors.add("Full name must be 2-" + AppConstants.MAX_NAME_LENGTH + " characters.");
        }
    }

    private void validateUsername(RegistrationForm form, List<String> errors) {
        String username = clean(form.getUsername());
        boolean validFormat = username.length() >= AppConstants.MIN_USERNAME_LENGTH
                && username.length() <= AppConstants.MAX_USERNAME_LENGTH
                && USERNAME_PATTERN.matcher(username).matches();

        if (!validFormat) {
            errors.add(String.format("Username must be %d-%d characters (letters, numbers, '.' or '_').",
                    AppConstants.MIN_USERNAME_LENGTH, AppConstants.MAX_USERNAME_LENGTH));
        } else if (userDAO.existsByUsername(username)) {
            errors.add("Username is already taken.");
        }
    }

    private void validateEmail(RegistrationForm form, List<String> errors) {
        String email = clean(form.getEmail());
        boolean validFormat = email.length() <= AppConstants.MAX_EMAIL_LENGTH
                && EMAIL_PATTERN.matcher(email).matches();

        if (!validFormat) {
            errors.add("Enter a valid email address.");
        } else if (userDAO.existsByEmail(email.toLowerCase())) {
            errors.add("This email is already registered.");
        }
    }

    private void validatePhone(RegistrationForm form, List<String> errors) {
        String phone = normalizePhone(form.getPhone());
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            errors.add("Enter a valid phone number (9-15 digits).");
        }
    }

    private void validatePassword(RegistrationForm form, List<String> errors) {
        String password = form.getPassword() == null ? "" : form.getPassword();
        if (password.length() < AppConstants.MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least " + AppConstants.MIN_PASSWORD_LENGTH + " characters.");
        } else if (!password.equals(form.getConfirmPassword())) {
            errors.add("Passwords do not match.");
        }
    }

    private User toResident(RegistrationForm form) {
        String phone = normalizePhone(form.getPhone());

        User user = new User();
        user.setFullName(clean(form.getFullName()));
        user.setUsername(clean(form.getUsername()));
        user.setEmail(clean(form.getEmail()).toLowerCase());
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setPasswordHash(form.getPassword());
        user.setRole(UserRole.RESIDENT);
        user.setActive(true);
        return user;
    }


    private boolean passwordMatches(String rawPassword, String storedPassword) {
        return rawPassword.equals(storedPassword);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePhone(String phone) {
        return clean(phone).replaceAll("[\\s-]", "");
    }
}