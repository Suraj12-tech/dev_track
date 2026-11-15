package backend.DevTrack.repository;

import backend.DevTrack.entity.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select verificationToken from EmailVerificationToken verificationToken where verificationToken.token = :token")
    Optional<EmailVerificationToken> findByToken(@Param("token") String token);
}
