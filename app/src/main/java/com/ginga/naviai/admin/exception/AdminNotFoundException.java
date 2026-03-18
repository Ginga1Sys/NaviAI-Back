package com.ginga.naviai.admin.exception;

import org.springframework.http.HttpStatus;

public class AdminNotFoundException extends AdminApiException {
    public AdminNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
