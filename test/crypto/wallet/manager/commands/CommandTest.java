package crypto.wallet.manager.commands;

import crypto.wallet.manager.exceptions.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandTest {

    @Test
    void newCommand_validInput_shouldCreateCommand() {
        String input = "LOGIN user123 pass123";

        Command command = Command.newCommand(input);

        assertEquals(CommandType.LOGIN, command.type(), "Command type should be LOGIN");
        assertArrayEquals(new String[]{"user123", "pass123"}, command.arguments(),
                "Command arguments should match the expected values");
    }

    @Test
    void newCommand_nullInput_shouldThrowParseException() {
        assertThrows(ParseException.class, () -> Command.newCommand(null),
                "Creating command with null input should throw ParseException");
    }

    @Test
    void newCommand_invalidCommandType_shouldCreateCommandWithUnknownType() {
        String input = "INVALID_TYPE arg1 arg2";

        Command command = Command.newCommand(input);

        assertEquals(CommandType.UNKNOWN, command.type(), "Command type should be UNKNOWN");
        assertArrayEquals(new String[]{"arg1", "arg2"}, command.arguments(),
                "Command arguments should match the expected values");
    }

    @Test
    void equals_sameCommands_shouldBeEqual() {
        Command command1 = new Command(CommandType.LOGIN, new String[]{"user123", "pass123"});
        Command command2 = new Command(CommandType.LOGIN, new String[]{"user123", "pass123"});

        assertEquals(command1, command2, "Identical commands should be equal");
    }

    @Test
    void equals_differentCommands_shouldNotBeEqual() {
        Command command1 = new Command(CommandType.LOGIN, new String[]{"user123", "pass123"});
        Command command2 = new Command(CommandType.REGISTER, new String[]{"user456", "pass456"});

        assertNotEquals(command1, command2, "Different commands should not be equal");
    }

    @Test
    void hashCode_sameCommands_shouldHaveSameHashCode() {
        Command command1 = new Command(CommandType.LOGIN, new String[]{"user123", "pass123"});
        Command command2 = new Command(CommandType.LOGIN, new String[]{"user123", "pass123"});

        assertEquals(command1.hashCode(), command2.hashCode(),
                "Identical commands should have the same hash code");
    }

    @Test
    void hashCode_differentCommands_shouldHaveDifferentHashCodes() {
        Command command1 = new Command(CommandType.LOGIN, new String[]{"user123", "pass123"});
        Command command2 = new Command(CommandType.REGISTER, new String[]{"user456", "pass456"});

        assertNotEquals(command1.hashCode(), command2.hashCode(),
                "Different commands should have different hash codes");
    }
}
