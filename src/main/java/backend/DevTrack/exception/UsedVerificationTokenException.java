package backend.DevTrack.exception;

public class UsedVerificationTokenException extends RuntimeException {

    public UsedVerificationTokenException() {
        super("Verification token has already been used");
    }
}
