package com.lofo.serenia.service.notification;

public interface EmailTemplateProvider {

    String getActivationEmailSubject();

    String getActivationEmailBody(String firstName, String activationLink);

    String getPasswordResetEmailSubject();

    String getPasswordResetEmailBody(String firstName, String resetLink);
}
