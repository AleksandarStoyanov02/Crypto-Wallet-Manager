package crypto.wallet.manager.commands;

import crypto.wallet.manager.exceptions.ParseException;

import java.util.Arrays;
import java.util.List;

public record Command(CommandType type, String[] arguments) {
    static final int REQUIRED_ARGUMENTS_FOR_LOGIN_REGISTER_BUY_CRYPTO = 2;
    static final int REQUIRED_ARGUMENTS_FOR_DEPOSIT_CRYPTO = 1;
    static final int REQUIRED_ARGUMENTS_FOR_SELLING_CRYPTO = 1;

    public static Command newCommand(String clientInput) {
        if (clientInput == null) {
            throw new ParseException("clientInput cannot be null");
        }

        List<String> tokens = Arrays.asList(clientInput.split(" "));

        if (tokens.isEmpty()) {
            throw new ParseException("Invalid command format");
        }

        String[] args = tokens.size() > 1 ? tokens.subList(1, tokens.size()).toArray(new String[0]) : null;
        CommandType type = CommandType.fromString(tokens.getFirst());
        return new Command(type, args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Command command1 = (Command) o;

        if (type != command1.type) return false;
        return Arrays.equals(arguments, command1.arguments);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
