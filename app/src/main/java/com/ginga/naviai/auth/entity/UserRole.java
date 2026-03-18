package com.ginga.naviai.auth.entity;

import java.util.Locale;

public enum UserRole {
    ADMIN,
    USER,
    MODERATOR;

    public String toApiValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static UserRole fromApiValue(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;
        return UserRole.valueOf(normalized);
    }
}
