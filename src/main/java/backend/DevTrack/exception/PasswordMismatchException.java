package backend.DevTrack.exception;

public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException() {
        super("Password and confirmation do not match");
    }
}
