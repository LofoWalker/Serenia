package com.lofo.serenia.validation.validator;

import com.lofo.serenia.validation.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for password policy.
 *
 * Rules:
 * - Minimum 8 characters
 * - At least 3 criteria among 4: uppercase, lowercase, digit, symbol
 */
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final int MIN_CRITERIA = 3;
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~\\";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }

        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            setCustomMessage(context, "Password must contain at least " + MIN_LENGTH + " characters");
            return false;
        }

        // Count met criteria
        int criteriaCount = 0;

        if (hasUppercase(password)) criteriaCount++;
        if (hasLowercase(password)) criteriaCount++;
        if (hasDigit(password)) criteriaCount++;
        if (hasSymbol(password)) criteriaCount++;

        if (criteriaCount < MIN_CRITERIA) {
            setCustomMessage(context,
                "Password must meet at least " + MIN_CRITERIA +
                " criteria among: uppercase, lowercase, digit, symbol");
            return false;
        }

        return true;
    }

    private boolean hasUppercase(String password) {
        return password.chars().anyMatch(Character::isUpperCase);
    }

    private boolean hasLowercase(String password) {
        return password.chars().anyMatch(Character::isLowerCase);
    }

    private boolean hasDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }

    private boolean hasSymbol(String password) {
        return password.chars().anyMatch(c -> SYMBOLS.indexOf(c) >= 0);
    }

    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}

