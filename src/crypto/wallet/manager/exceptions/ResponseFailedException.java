package crypto.wallet.manager.exceptions;

public class ResponseFailedException extends RuntimeException {
    public ResponseFailedException(String message) {
        super(message);
    }

    public ResponseFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
