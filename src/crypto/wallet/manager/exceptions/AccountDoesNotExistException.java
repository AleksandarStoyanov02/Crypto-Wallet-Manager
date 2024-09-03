package crypto.wallet.manager.exceptions;

public class AccountDoesNotExistException extends Exception {
    public AccountDoesNotExistException(String message) {
        super(message);
    }

    public AccountDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
