package backend.DevTrack.exception;

public class ExpiredVerificationTokenException extends RuntimeException {

    public ExpiredVerificationTokenException() {
        super("Verification token has expired");
    }
}
