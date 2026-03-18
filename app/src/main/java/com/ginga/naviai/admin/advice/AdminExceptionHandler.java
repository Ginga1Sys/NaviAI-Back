package com.ginga.naviai.admin.advice;

import com.ginga.naviai.admin.exception.AdminApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.ginga.naviai.admin")
public class AdminExceptionHandler {

    @ExceptionHandler(AdminApiException.class)
    public ResponseEntity<?> handleAdmin(AdminApiException ex) {
        Map<String, Object> err = new HashMap<>();
        err.put("code", ex.getCode());
        err.put("message", ex.getMessage() == null ? "Error" : ex.getMessage());
        if (ex.getDetails() != null) err.put("details", ex.getDetails());
        return ResponseEntity.status(ex.getStatus()).body(Map.of("error", err));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError f : ex.getBindingResult().getFieldErrors()) {
            details.put(f.getField(), f.getDefaultMessage());
        }
        Map<String, Object> err = new HashMap<>();
        err.put("code", "INVALID_INPUT");
        err.put("message", "Validation failed");
        err.put("details", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", err));
    }
}
