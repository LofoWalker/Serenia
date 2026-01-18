package com.lofo.serenia.service.mail;

import com.lofo.serenia.service.mail.sender.QuarkusMailSender;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuarkusMailSender Unit Tests")
class QuarkusMailSenderTest {

    @Mock
    private Mailer mailer;

    private QuarkusMailSender quarkusMailSender;

    @BeforeEach
    void setUp() {
        quarkusMailSender = new QuarkusMailSender(mailer);
    }

    @Nested
    @DisplayName("sendHtml")
    class SendHtml {

        @Test
        @DisplayName("should send html email with correct parameters")
        void should_send_html_email_with_correct_parameters() {
            String to = "recipient@example.com";
            String subject = "Test Subject";
            String htmlContent = "<html><body><h1>Hello</h1></body></html>";

            quarkusMailSender.sendHtml(to, subject, htmlContent);

            ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
            verify(mailer).send(mailCaptor.capture());

            Mail capturedMail = mailCaptor.getValue();
            assertThat(capturedMail.getTo()).contains(to);
            assertThat(capturedMail.getSubject()).isEqualTo(subject);
            assertThat(capturedMail.getHtml()).isEqualTo(htmlContent);
        }

        @Test
        @DisplayName("should call mailer send")
        void should_call_mailer_send() {
            String to = "test@example.com";
            String subject = "Welcome";
            String htmlContent = "<p>Welcome!</p>";

            quarkusMailSender.sendHtml(to, subject, htmlContent);

            verify(mailer).send((Mail) org.mockito.ArgumentMatchers.any());
        }
    }
}
