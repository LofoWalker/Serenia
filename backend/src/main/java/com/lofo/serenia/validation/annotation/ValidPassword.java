package com.lofo.serenia.validation.annotation;

import com.lofo.serenia.validation.validator.PasswordConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation for passwords.
 *
 * Rules:
 * - Minimum 8 characters
 * - At least 3 criteria among 4: uppercase, lowercase, digit, symbol
 */
@Documented
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Password does not meet security policy requirements";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

