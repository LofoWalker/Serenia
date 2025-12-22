package com.lofo.serenia.service.mail;

/**
 * Interface for sending emails.
 * Abstracts the email sending mechanism to allow different implementations.
 */
public interface MailSender {

    /**
     * Sends an HTML email.
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param htmlContent the HTML content of the email
     */
    void sendHtml(String to, String subject, String htmlContent);
}

