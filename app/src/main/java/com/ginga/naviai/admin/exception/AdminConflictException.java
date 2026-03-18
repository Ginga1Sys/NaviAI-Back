package com.ginga.naviai.admin.exception;

import org.springframework.http.HttpStatus;

public class AdminConflictException extends AdminApiException {
    public AdminConflictException(String message) {
        this("INVALID_STATUS_TRANSITION", message);
    }

    public AdminConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
