package com.lofo.serenia.service.mail;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Provides email templates for various email types.
 * Loads templates from resources and handles placeholder substitution.
 */
@ApplicationScoped
public class EmailTemplateProvider {

    private static final String ACTIVATION_TEMPLATE_PATH = "templates/email/activation-email.template";
    private static final String PASSWORD_RESET_TEMPLATE_PATH = "templates/email/password-reset-email.template";

    private String activationEmailTemplate;
    private String passwordResetEmailTemplate;

    @PostConstruct
    void init() {
        this.activationEmailTemplate = loadTemplate(ACTIVATION_TEMPLATE_PATH);
        this.passwordResetEmailTemplate = loadTemplate(PASSWORD_RESET_TEMPLATE_PATH);
    }

    private String loadTemplate(String templatePath) {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(templatePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Template not found: " + templatePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load email template: " + templatePath, e);
        }
    }

    public String getActivationEmailSubject() {
        return "Serenia - Bienvenue parmi nous";
    }

    public String getActivationEmailBody(String firstName, String activationLink) {
        return activationEmailTemplate
                .replace("{{firstName}}", firstName)
                .replace("{{activationLink}}", activationLink);
    }

    public String getPasswordResetEmailSubject() {
        return "Serenia - Réinitialisation de mot de passe";
    }

    public String getPasswordResetEmailBody(String firstName, String resetLink) {
        return passwordResetEmailTemplate
                .replace("{{firstName}}", firstName)
                .replace("{{resetLink}}", resetLink);
    }
}

