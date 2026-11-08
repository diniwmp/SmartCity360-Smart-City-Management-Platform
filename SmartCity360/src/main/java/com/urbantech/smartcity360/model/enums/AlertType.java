package com.urbantech.smartcity360.model.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum AlertType {
    COMPLAINT("Complaint", "bi-chat-left-text", UserRole.ADMIN, UserRole.CITY_MANAGER, UserRole.SUPERVISOR),
    ENERGY("Energy", "bi-lightning-charge", UserRole.ADMIN, UserRole.CITY_MANAGER, UserRole.SUPERVISOR),
    BIN_OVERFLOW("Bin overflow", "bi-trash", UserRole.ADMIN, UserRole.CITY_MANAGER, UserRole.SUPERVISOR, UserRole.FIELD_STAFF),
    SENSOR("Sensor", "bi-broadcast", UserRole.ADMIN, UserRole.CITY_MANAGER),
    POLICY("Policy", "bi-journal-code", UserRole.ADMIN, UserRole.CITY_MANAGER);

    private final String label;
    private final String icon;
    private final Set<UserRole> recipientRoles;

    AlertType(String label, String icon, UserRole... roles) {
        this.label = label;
        this.icon = icon;
        this.recipientRoles = EnumSet.copyOf(Arrays.asList(roles));
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }

    public Set<UserRole> getRecipientRoles() {
        return Collections.unmodifiableSet(recipientRoles);
    }
}