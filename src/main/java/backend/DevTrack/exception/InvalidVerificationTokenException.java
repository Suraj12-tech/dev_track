package backend.DevTrack.exception;

public class InvalidVerificationTokenException extends RuntimeException {

    public InvalidVerificationTokenException() {
        super("Verification token is invalid");
    }
}
