package backend.DevTrack.service;

import backend.DevTrack.entity.EmailVerificationToken;
import backend.DevTrack.entity.User;
import backend.DevTrack.exception.ExpiredVerificationTokenException;
import backend.DevTrack.exception.InvalidVerificationTokenException;
import backend.DevTrack.exception.UsedVerificationTokenException;
import backend.DevTrack.repository.EmailVerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final long expirationSeconds;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            EmailService emailService,
            @Value("${app.email-verification.expiration}") long expirationSeconds) {
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional
    public void createAndSendVerificationEmail(User user) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        LocalDateTime createdAt = LocalDateTime.now();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(hashToken(rawToken));
        verificationToken.setCreatedAt(createdAt);
        verificationToken.setExpiresAt(createdAt.plusSeconds(expirationSeconds));
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), rawToken);
    }

    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidVerificationTokenException();
        }

        EmailVerificationToken verificationToken = tokenRepository.findByToken(hashToken(rawToken))
                .orElseThrow(InvalidVerificationTokenException::new);

        if (verificationToken.getUsedAt() != null) {
            throw new UsedVerificationTokenException();
        }

        if (!verificationToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ExpiredVerificationTokenException();
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        verificationToken.setUsedAt(LocalDateTime.now());
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
