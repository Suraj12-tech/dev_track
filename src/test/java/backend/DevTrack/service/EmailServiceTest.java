package backend.DevTrack.service;

import backend.DevTrack.exception.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    @Test
    void sendsVerificationLinkWithExpiryInformation() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, "sender@example.com",
                "http://localhost:8080", 86400);

        emailService.sendVerificationEmail("person@example.com", "raw-token");

        org.mockito.ArgumentCaptor<SimpleMailMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("sender@example.com", message.getFrom());
        org.junit.jupiter.api.Assertions.assertEquals("person@example.com", message.getTo()[0]);
        org.junit.jupiter.api.Assertions.assertTrue(message.getText().contains(
                "http://localhost:8080/api/auth/verify-email?token=raw-token"));
        org.junit.jupiter.api.Assertions.assertTrue(message.getText().contains("24 hours"));
    }

    @Test
    void reportsMailSenderFailure() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailAuthenticationException("mail server failure"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailService emailService = new EmailService(mailSender, "sender@example.com",
                "http://localhost:8080", 86400);

        assertThrows(EmailDeliveryException.class,
                () -> emailService.sendVerificationEmail("person@example.com", "raw-token"));
    }
}
