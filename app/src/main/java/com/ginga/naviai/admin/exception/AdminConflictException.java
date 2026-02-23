package com.ginga.naviai.admin.exception;

import org.springframework.http.HttpStatus;

public class AdminConflictException extends AdminApiException {
    public AdminConflictException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", message);
    }
}
