package backend.DevTrack.exception;

public class PasswordTooLongException extends RuntimeException {

    public PasswordTooLongException() {
        super("Password must be at most 72 UTF-8 bytes");
    }
}
