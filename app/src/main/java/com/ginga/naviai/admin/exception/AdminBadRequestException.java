package com.ginga.naviai.admin.exception;

import org.springframework.http.HttpStatus;

public class AdminBadRequestException extends AdminApiException {
    public AdminBadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }

    public AdminBadRequestException(String message, Object details) {
        super(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message, details);
    }
}
