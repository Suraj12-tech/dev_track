package backend.DevTrack.exception;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException() {
        super("Could not send the verification email. Please try again later.");
    }
}
