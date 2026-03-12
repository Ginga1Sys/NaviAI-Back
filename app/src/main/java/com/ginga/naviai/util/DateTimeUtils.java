package com.ginga.naviai.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        throw new IllegalArgumentException("Unsupported datetime value type: " + value.getClass().getName());
    }
}