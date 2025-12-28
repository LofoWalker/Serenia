package com.lofo.serenia.validation.annotation;

import com.lofo.serenia.validation.validator.PasswordConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation de validation pour les mots de passe.
 *
 * Règles :
 * - Minimum 8 caractères
 * - Au moins 3 critères parmi 4 : majuscule, minuscule, chiffre, symbole
 */
@Documented
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Le mot de passe ne respecte pas la politique de sécurité";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

