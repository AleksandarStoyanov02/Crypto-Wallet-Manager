package crypto.wallet.manager.exceptions;

public class AccountIsAlreadyLoggedInException extends Exception {
    public AccountIsAlreadyLoggedInException(String message) {
        super(message);
    }

    public AccountIsAlreadyLoggedInException(String message, Throwable cause) {
        super(message, cause);
    }
}
