package crypto.wallet.manager.commands;

public enum CommandErrorMessageType {
    ACCOUNT_EXISTS("Account already exists"),
    ALREADY_LOGGED_IN("You are already logged in."),
    ALREADY_LOGGED_IN_REGISTER_MESSAGE("Cannot make a new account while already logged in."),
    CRYPTO_COIN_DOES_NOT_EXIST("No crypto coin exists with this id."),
    CRYPTO_COIN_STORAGE_PROBLEM_LOG_MESSAGE("There is a problem with the crypto coin storage of user "),
    DISCONNECTED("disconnected"),
    INSUFFICIENT_AMOUNT("Not enough balance in your wallet."),
    INVALID_FORMAT_AMOUNT("Invalid format for the amount of money."),
    INVALID_INPUT_ARGUMENTS("Invalid command"),
    INVALID_LOGIN("Wrong username or password."),
    LOGIN_SUCCESSFUL("Login successful"),
    MUST_LOGIN("Log in first or create a new account if you don't have one."),
    NEGATIVE_AMOUNT("Amount cannot be negative."),
    PROBLEM_WHILE_LOGGING_IN("A problem occurred while trying to log in. Try again."),
    REGISTER_SUCCESSFUL("Register successful"),
    SERVER_SIDE_ERROR("An error occurred on the server. Try again later."),
    SUCCESSFUL_OPERATION("Transaction completed"),
    UNKNOWN_COMMAND_MESSAGE("Unknown command");

    private final String message;

    CommandErrorMessageType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}