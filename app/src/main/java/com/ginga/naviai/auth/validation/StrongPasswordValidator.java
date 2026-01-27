package com.ginga.naviai.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public void initialize(StrongPassword constraintAnnotation) { }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        int categories = 0;
        if (value.matches(".*[A-Z].*")) categories++;
        if (value.matches(".*[a-z].*")) categories++;
        if (value.matches(".*[0-9].*")) categories++;
        if (value.matches(".*[^A-Za-z0-9].*")) categories++;
        return categories >= 3 && value.length() >= 8;
    }
}
