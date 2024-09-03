package crypto.wallet.manager.exceptions;

public class InvalidCryptoCoinException extends Exception {
    public InvalidCryptoCoinException(String message) {
        super(message);
    }

    public InvalidCryptoCoinException(String message, Throwable cause) {
        super(message, cause);
    }
}
