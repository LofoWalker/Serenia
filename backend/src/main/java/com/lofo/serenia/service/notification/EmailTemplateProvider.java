package com.lofo.serenia.service.notification;

public interface EmailTemplateProvider {

    String getActivationEmailSubject();

    String getActivationEmailBody(String firstName, String activationLink);
}
