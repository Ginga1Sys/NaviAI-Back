package com.ginga.naviai.knowledge.entity;

import java.util.Locale;

public enum KnowledgeStatus {
    DRAFT,
    PENDING,
    PUBLISHED,
    DECLINED,
    ARCHIVED;

    public String toApiValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static KnowledgeStatus fromApiValue(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;
        return KnowledgeStatus.valueOf(normalized);
    }
}
