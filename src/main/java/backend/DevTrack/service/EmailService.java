package backend.DevTrack.service;

import backend.DevTrack.exception.EmailDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String senderAddress;
    private final String baseUrl;
    private final long expirationSeconds;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderAddress,
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.email-verification.expiration}") long expirationSeconds) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
        this.baseUrl = baseUrl;
        this.expirationSeconds = expirationSeconds;
    }

    public void sendVerificationEmail(String recipient, String rawToken) {
        String verificationLink = baseUrl + "/api/auth/verify-email?token=" + rawToken;
        long expirationHours = expirationSeconds / 3600;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(recipient);
        message.setSubject("Verify your DevTrack email");
        message.setText(
                "Welcome to DevTrack!\n\n"
                        + "Please verify your email address by opening this link:\n"
                        + verificationLink
                        + "\n\nThis link expires in "
                        + expirationHours
                        + " hours. If you did not create a DevTrack account, you can ignore this email.");

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new EmailDeliveryException();
        }
    }
}
