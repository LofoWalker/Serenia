package com.lofo.serenia.validation.validator;

import com.lofo.serenia.validation.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validateur pour la politique de mot de passe.
 *
 * Règles :
 * - Minimum 8 caractères
 * - Au moins 3 critères parmi 4 : majuscule, minuscule, chiffre, symbole
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

        // Vérifier la longueur minimale
        if (password.length() < MIN_LENGTH) {
            setCustomMessage(context, "Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères");
            return false;
        }

        // Compter les critères respectés
        int criteriaCount = 0;

        if (hasUppercase(password)) criteriaCount++;
        if (hasLowercase(password)) criteriaCount++;
        if (hasDigit(password)) criteriaCount++;
        if (hasSymbol(password)) criteriaCount++;

        if (criteriaCount < MIN_CRITERIA) {
            setCustomMessage(context,
                "Le mot de passe doit respecter au moins " + MIN_CRITERIA +
                " critères parmi : majuscule, minuscule, chiffre, symbole");
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

