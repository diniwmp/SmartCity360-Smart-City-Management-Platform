package com.urbantech.smartcity360.dto;

import com.urbantech.smartcity360.model.User;
import com.urbantech.smartcity360.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class LoggedInUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String fullName;
    private final String username;
    private final UserRole role;

    public static LoggedInUser from(User user) {
        return new LoggedInUser(user.getId(), user.getFullName(), user.getUsername(), user.getRole());
    }

    public String getRoleLabel() {
        return Arrays.stream(role.name().toLowerCase().split("_"))
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    public String getInitial() {
        return fullName == null || fullName.isBlank() ? "?" : fullName.substring(0, 1).toUpperCase();
    }
}