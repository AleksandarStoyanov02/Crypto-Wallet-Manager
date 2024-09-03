package crypto.wallet.manager.exceptions;

public class AccountNotLoggedInException extends RuntimeException {
    public AccountNotLoggedInException(String message) {
        super(message);
    }

    public AccountNotLoggedInException(String message, Throwable cause) {
        super(message, cause);
    }
}
