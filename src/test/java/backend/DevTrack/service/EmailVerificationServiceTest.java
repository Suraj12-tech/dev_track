package backend.DevTrack.service;

import backend.DevTrack.entity.EmailVerificationToken;
import backend.DevTrack.entity.User;
import backend.DevTrack.exception.EmailDeliveryException;
import backend.DevTrack.exception.ExpiredVerificationTokenException;
import backend.DevTrack.exception.InvalidVerificationTokenException;
import backend.DevTrack.exception.UsedVerificationTokenException;
import backend.DevTrack.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    private EmailVerificationTokenRepository tokenRepository;
    private EmailService emailService;
    private EmailVerificationService verificationService;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(EmailVerificationTokenRepository.class);
        emailService = mock(EmailService.class);
        verificationService = new EmailVerificationService(tokenRepository, emailService, 86400);
    }

    @Test
    void createsAndEmailsSecureTokenButStoresOnlyItsHash() {
        User user = new User();
        user.setEmail("person@example.com");

        verificationService.createAndSendVerificationEmail(user);

        org.mockito.ArgumentCaptor<EmailVerificationToken> tokenCaptor =
                org.mockito.ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        EmailVerificationToken savedToken = tokenCaptor.getValue();

        org.mockito.ArgumentCaptor<String> emailTokenCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(
                org.mockito.ArgumentMatchers.eq("person@example.com"), emailTokenCaptor.capture());
        String rawToken = emailTokenCaptor.getValue();

        assertNotEquals(rawToken, savedToken.getToken());
        assertEquals(EmailVerificationService.hashToken(rawToken), savedToken.getToken());
        assertEquals(savedToken.getCreatedAt().plusSeconds(86400), savedToken.getExpiresAt());
        assertEquals(user, savedToken.getUser());
    }

    @Test
    void verifiesValidTokenAndMarksItUsed() {
        User user = new User();
        user.setEmailVerified(false);
        EmailVerificationToken token = tokenFor(user, LocalDateTime.now().plusHours(1), null);
        when(tokenRepository.findByToken(EmailVerificationService.hashToken("raw-token")))
                .thenReturn(Optional.of(token));

        verificationService.verify("raw-token");

        assertTrue(user.isEmailVerified());
        assertTrue(token.getUsedAt() != null);
    }

    @Test
    void rejectsUnknownToken() {
        when(tokenRepository.findByToken(any())).thenReturn(Optional.empty());

        assertThrows(InvalidVerificationTokenException.class, () -> verificationService.verify("unknown"));
    }

    @Test
    void rejectsExpiredToken() {
        EmailVerificationToken token = tokenFor(
                new User(), LocalDateTime.now().minusSeconds(1), null);
        when(tokenRepository.findByToken(EmailVerificationService.hashToken("expired")))
                .thenReturn(Optional.of(token));

        assertThrows(ExpiredVerificationTokenException.class, () -> verificationService.verify("expired"));
        assertFalse(token.getUser().isEmailVerified());
    }

    @Test
    void rejectsAlreadyUsedToken() {
        EmailVerificationToken token = tokenFor(
                new User(), LocalDateTime.now().plusHours(1), LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken(EmailVerificationService.hashToken("used")))
                .thenReturn(Optional.of(token));

        assertThrows(UsedVerificationTokenException.class, () -> verificationService.verify("used"));
        assertFalse(token.getUser().isEmailVerified());
    }

    @Test
    void emailFailureIsPropagatedAndDoesNotReportSuccess() {
        User user = new User();
        user.setEmail("person@example.com");
        doThrow(new EmailDeliveryException()).when(emailService)
                .sendVerificationEmail(any(), any());

        assertThrows(EmailDeliveryException.class,
                () -> verificationService.createAndSendVerificationEmail(user));

        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    private EmailVerificationToken tokenFor(User user, LocalDateTime expiresAt, LocalDateTime usedAt) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken("stored-hash");
        token.setCreatedAt(LocalDateTime.now().minusHours(1));
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        return token;
    }
}
