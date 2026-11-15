package backend.DevTrack.dto;

import backend.DevTrack.enums.Role;

import java.time.LocalDateTime;

public class RegisterResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final boolean emailVerified;
    private final Role accountRole;
    private final LocalDateTime createdAt;

    public RegisterResponse(
            Long id,
            String name,
            String email,
            boolean emailVerified,
            Role accountRole,
            LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.emailVerified = emailVerified;
        this.accountRole = accountRole;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Role getAccountRole() {
        return accountRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
