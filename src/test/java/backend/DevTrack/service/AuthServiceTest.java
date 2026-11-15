package backend.DevTrack.service;

import backend.DevTrack.dto.RegisterRequest;
import backend.DevTrack.dto.RegisterResponse;
import backend.DevTrack.entity.User;
import backend.DevTrack.enums.Role;
import backend.DevTrack.exception.DuplicateEmailException;
import backend.DevTrack.exception.PasswordMismatchException;
import backend.DevTrack.repository.EmailVerificationTokenRepository;
import backend.DevTrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private EmailVerificationService emailVerificationService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        emailVerificationService = mock(EmailVerificationService.class);
        authService = new AuthService(userRepository, passwordEncoder, emailVerificationService);
    }

    @Test
    void registerSavesEncodedPasswordAndReturnsSafeResponse() {
        EmailVerificationTokenRepository tokenRepository = mock(EmailVerificationTokenRepository.class);
        EmailService emailService = mock(EmailService.class);
        EmailVerificationService actualVerificationService =
                new EmailVerificationService(tokenRepository, emailService, 86400);
        AuthService registrationService =
                new AuthService(userRepository, passwordEncoder, actualVerificationService);

        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = registrationService.register(
                request("  Person  ", " PERSON@EXAMPLE.COM ", "securePass123", "securePass123"));

        assertEquals("Person", response.getName());
        assertEquals("person@example.com", response.getEmail());
        assertFalse(response.isEmailVerified());
        assertEquals(Role.USER, response.getAccountRole());

        org.mockito.ArgumentCaptor<User> savedUserCaptor =
                org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();
        assertNotEquals("securePass123", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("securePass123", savedUser.getPassword()));

        org.mockito.ArgumentCaptor<backend.DevTrack.entity.EmailVerificationToken> tokenCaptor =
                org.mockito.ArgumentCaptor.forClass(backend.DevTrack.entity.EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        org.mockito.ArgumentCaptor<String> rawTokenCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(
                org.mockito.ArgumentMatchers.eq("person@example.com"), rawTokenCaptor.capture());
        assertEquals(EmailVerificationService.hashToken(rawTokenCaptor.getValue()),
                tokenCaptor.getValue().getToken());
        assertEquals(savedUser, tokenCaptor.getValue().getUser());
    }

    @Test
    void registerRejectsAnExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () ->
                authService.register(request("Person", "person@example.com", "securePass123", "securePass123")));

        verify(userRepository, never()).save(any(User.class));
        verify(emailVerificationService, never()).createAndSendVerificationEmail(any());
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        assertThrows(PasswordMismatchException.class, () ->
                authService.register(request("Person", "person@example.com", "securePass123", "differentPass123")));

        verify(userRepository, never()).existsByEmailIgnoreCase(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerPropagatesVerificationEmailFailure() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new backend.DevTrack.exception.EmailDeliveryException())
                .when(emailVerificationService).createAndSendVerificationEmail(any(User.class));

        assertThrows(backend.DevTrack.exception.EmailDeliveryException.class, () ->
                authService.register(request("Person", "person@example.com", "securePass123", "securePass123")));
    }

    @Test
    void registerRejectsPasswordOverBcryptByteLimit() {
        assertThrows(backend.DevTrack.exception.PasswordTooLongException.class, () ->
                authService.register(request("Person", "person@example.com", "é".repeat(37), "é".repeat(37))));

        verify(userRepository, never()).save(any(User.class));
    }

    private RegisterRequest request(String name, String email, String password, String confirmPassword) {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        request.setConfirmPassword(confirmPassword);
        return request;
    }
}
