package com.lofo.serenia.service.mail.sender;

import com.lofo.serenia.service.mail.MailSender;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Quarkus implementation of MailSender.
 * Uses Quarkus Mailer to send emails.
 */
@Slf4j
@ApplicationScoped
public class QuarkusMailSender implements MailSender {

    private final Mailer mailer;

    @Inject
    public QuarkusMailSender(Mailer mailer) {
        this.mailer = mailer;
    }

    @Override
    public void sendHtml(String to, String subject, String htmlContent) {
        log.debug("Sending email to={}, subject={}", to, subject);
        Mail mail = Mail.withHtml(to, subject, htmlContent);
        mailer.send(mail);
        log.info("Email sent successfully to {}", to);
    }
}

