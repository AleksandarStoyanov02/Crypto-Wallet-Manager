package crypto.wallet.manager.commands;

import crypto.wallet.manager.account.Account;
import crypto.wallet.manager.database.CryptoCoinsDatabase;
import crypto.wallet.manager.database.UserAccountsDatabase;
import crypto.wallet.manager.exceptions.AccountAlreadyExistsException;
import crypto.wallet.manager.exceptions.AccountDoesNotExistException;
import crypto.wallet.manager.exceptions.AccountIsAlreadyLoggedInException;
import crypto.wallet.manager.exceptions.InsufficientBalanceException;
import crypto.wallet.manager.exceptions.InvalidCryptoCoinException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.channels.SelectionKey;
import java.util.Optional;

import static crypto.wallet.manager.commands.CommandErrorMessageType.ALREADY_LOGGED_IN;
import static crypto.wallet.manager.commands.CommandErrorMessageType.CRYPTO_COIN_DOES_NOT_EXIST;
import static crypto.wallet.manager.commands.CommandErrorMessageType.DISCONNECTED;
import static crypto.wallet.manager.commands.CommandErrorMessageType.INVALID_INPUT_ARGUMENTS;
import static crypto.wallet.manager.commands.CommandErrorMessageType.INVALID_LOGIN;
import static crypto.wallet.manager.commands.CommandErrorMessageType.LOGIN_SUCCESSFUL;
import static crypto.wallet.manager.commands.CommandErrorMessageType.REGISTER_SUCCESSFUL;
import static crypto.wallet.manager.commands.CommandErrorMessageType.UNKNOWN_COMMAND_MESSAGE;
import static crypto.wallet.manager.commands.CommandType.SHUTDOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommandExecutorTest {
    private static final UserAccountsDatabase userAccountsDatabase = mock(UserAccountsDatabase.class);
    private static final CryptoCoinsDatabase cryptoCoinsDatabase = mock(CryptoCoinsDatabase.class);
    private static final SelectionKey key = mock(SelectionKey.class);
    private static CommandExecutor commandExecutor;
    private final Account mockedAccount = mock(Account.class);
    private Account account;

    @BeforeEach
    void setUp() {
        commandExecutor = CommandExecutor.getInstance(userAccountsDatabase, cryptoCoinsDatabase);
        account = new Account("user", "1");
        key.attach(account);
    }

    @AfterEach
    void tearDown() {
        // Ensure that the key does not have any attached account
        if (key.attachment() != null) {
            key.attach(null);
        }

        // Optionally, reset mocks if required
        Mockito.reset(userAccountsDatabase, cryptoCoinsDatabase, mockedAccount, key);
    }

    @Test
    public void testLoginAccountDoesNotExist() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        key.attach(null);
        when(userAccountsDatabase.login(anyString(), anyString())).thenThrow(AccountDoesNotExistException.class);

        assertEquals(INVALID_LOGIN.getMessage(), commandExecutor
                .execute(Command.newCommand("login user1 1"), key),
                "Expected INVALID_LOGIN message when the account does not exist.");
    }

    @Test
    public void testLoginTooManyArguments() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        key.attach(null);
        when(userAccountsDatabase.login("user", "1")).thenThrow(AccountDoesNotExistException.class);

        assertEquals(INVALID_INPUT_ARGUMENTS.getMessage(), commandExecutor
                .execute(Command.newCommand("login user 1 1"), key),
                "Expected INVALID_INPUT_ARGUMENTS message when there are too many arguments.");
    }

    @Test
    public void testExecuteLoginAccountAlreadyLoggedIn() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        Account loggedInAccount = new Account("user", "1");

        when(userAccountsDatabase.login(anyString(), anyString())).thenReturn(loggedInAccount);

        String firstResult = commandExecutor.execute(Command.newCommand("login user 1"), key);
        assertEquals(LOGIN_SUCCESSFUL.getMessage(), firstResult,
                "Expected LOGIN_SUCCESSFUL message on successful login.");

        key.attach(loggedInAccount);

        when(userAccountsDatabase.login(anyString(), anyString())).thenThrow(AccountIsAlreadyLoggedInException.class);

        String secondResult = commandExecutor.execute(Command.newCommand("login user 1"), key);
        assertEquals(ALREADY_LOGGED_IN.getMessage(), secondResult,
                "Expected ALREADY_LOGGED_IN message when the account is already logged in.");
    }

    @Test
    void testLogin() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException, AccountAlreadyExistsException {
        Account loggedInAccount = new Account("user", "1");
        when(userAccountsDatabase.login(anyString(), anyString())).thenReturn(loggedInAccount);

        String result = commandExecutor.execute(Command.newCommand("login user 1"), key);

        assertEquals(LOGIN_SUCCESSFUL.getMessage(), result,
                "Expected LOGIN_SUCCESSFUL message when login is successful.");
    }

    @Test
    void testUnknownCommand() {
        Command unknownCommand = new Command(CommandType.UNKNOWN, null);

        String result = commandExecutor.execute(unknownCommand, key);

        assertEquals(UNKNOWN_COMMAND_MESSAGE.getMessage(), result,
                "Expected UNKNOWN_COMMAND_MESSAGE when an unknown command is issued.");
    }

    @Test
    void testBuyCryptoNotLoggedIn() {
        key.attach(null);

        assertEquals("Log in first or create a new account if you don't have one.",
                commandExecutor.execute(Command.newCommand("buy_crypto BTC 50"), key),
                "Expected message indicating that login is required before buying crypto.");
    }

    @Test
    void testBuyCrypto() throws InsufficientBalanceException, InvalidCryptoCoinException {
        Account loggedInAccount = new Account("user", "1");
        when(userAccountsDatabase.getLoggedAccount(loggedInAccount)).thenReturn(mockedAccount);

        doNothing().when(mockedAccount).buyCryptoCoin(anyDouble(), anyString(), eq(cryptoCoinsDatabase));

        key.attach(loggedInAccount);

        String result = commandExecutor.execute(Command.newCommand("buy_crypto BTC 50"), key);

        assertEquals("Transaction completed", result,
                "Expected 'Transaction completed' when buying crypto successfully.");

        verify(mockedAccount).buyCryptoCoin(50.0, "BTC", cryptoCoinsDatabase);
    }

    @Test
    void testSellCryptoCryptoCoinDoesNotExist() {
        Command sellCryptoCommand = new Command(CommandType.SELL_CRYPTO, new String[]{"BTC"});
        Account mockAccount = new Account("username", "password");

        when(key.attachment()).thenReturn(mockAccount);
        when(userAccountsDatabase.getLoggedAccount(mockAccount)).thenReturn(mockAccount); // Fix here
        when(cryptoCoinsDatabase.findCryptoCoinByOfferingCode("BTC")).thenReturn(Optional.empty());

        String result = commandExecutor.execute(sellCryptoCommand, key);

        assertEquals(CRYPTO_COIN_DOES_NOT_EXIST.getMessage(), result,
                "Expected CRYPTO_COIN_DOES_NOT_EXIST message when the crypto coin does not exist.");
    }

    @Test
    void testSellCryptoNotLoggedIn() {
        key.attach(null);

        assertEquals("Log in first or create a new account if you don't have one.",
                commandExecutor.execute(Command.newCommand("sell_crypto"), key),
                "Expected message indicating that login is required before selling crypto.");
    }

    @Test
    void testSellCryptoTooManyArguments() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        Account loggedInAccount = new Account("user", "1");

        when(userAccountsDatabase.login(anyString(), anyString())).thenReturn(loggedInAccount);

        String firstResult = commandExecutor.execute(Command.newCommand("login user 1"), key);
        assertEquals(LOGIN_SUCCESSFUL.getMessage(), firstResult,
                "Expected LOGIN_SUCCESSFUL message on successful login.");

        key.attach(loggedInAccount);

        String result = commandExecutor.execute(Command.newCommand("sell_crypto BTC extra_arg1 extra_arg2"), key);

        assertEquals(INVALID_INPUT_ARGUMENTS.getMessage(), result,
                "Expected INVALID_INPUT_ARGUMENTS message when there are too many arguments.");
    }

    @Test
    public void testSellCryptoSuccessful() throws InvalidCryptoCoinException {
        when(userAccountsDatabase.getLoggedAccount(account)).thenReturn(mockedAccount);
        doNothing().when(mockedAccount).sellCryptoCoin(anyString(), eq(cryptoCoinsDatabase));

        assertEquals("Transaction completed",
                commandExecutor.execute(Command.newCommand("sell_crypto BTC"), key),
                "Expected 'Transaction completed' when selling crypto is successful.");
    }

    @Test
    void testDisconnect() {
        Command disconnectCommand = new Command(CommandType.DISCONNECT, null);
        Account mockAccount = new Account("username", "password");

        when(key.attachment()).thenReturn(mockAccount);

        String result = commandExecutor.execute(disconnectCommand, key);

        assertEquals(DISCONNECTED.getMessage(), result,
                "Expected DISCONNECTED message when disconnect command is executed.");
    }

    @Test
    void testShutdownCommand() {
        Command shutdownCommand = new Command(SHUTDOWN, null);

        String result = commandExecutor.execute(shutdownCommand, key);

        assertEquals(SHUTDOWN.toString(), result,
                "Expected SHUTDOWN message when shutdown command is executed.");
    }

    @Test
    void testHelpCommand() {
        Command helpCommand = new Command(CommandType.HELP, null);

        String result = commandExecutor.execute(helpCommand, key);

        assertTrue(result.contains("login"));
        assertTrue(result.contains("register"));
        assertTrue(result.contains("deposit"));
        assertTrue(result.contains("list_cryptos"));
        assertTrue(result.contains("buy_crypto"));
        assertTrue(result.contains("sell_crypto"));
        assertTrue(result.contains("wallet_information"));
        assertTrue(result.contains("wallet_investment_information"));
        assertTrue(result.contains("disconnect"));
    }

    @Test
    void testRegisterCommand() throws AccountAlreadyExistsException {
        Command registerCommand = new Command(CommandType.REGISTER, new String[]{"username", "password"});

        when(key.attachment()).thenReturn(null);
        doNothing().when(userAccountsDatabase).createAccount(any());

        String result = commandExecutor.execute(registerCommand, key);

        assertEquals(REGISTER_SUCCESSFUL.getMessage(), result,
                "Expected REGISTER_SUCCESSFUL message when a new account is successfully registered.");
        verify(userAccountsDatabase).createAccount(any());
    }

    @Test
    public void testExecuteListOfferingsLoggedIn() {
        when(cryptoCoinsDatabase.listOfferings()).thenReturn("Test output");

        assertEquals("Test output",
                commandExecutor.execute(Command.newCommand("list_cryptos"), key),
                "Expected 'Test output' when listing crypto offerings while logged in.");
    }

    @Test
    void testWalletInformationCommand() {
        when(userAccountsDatabase.getLoggedAccount(account)).thenReturn(mockedAccount);
        when(mockedAccount.walletInformation()).thenReturn("Test message");

        assertEquals("Test message",
                commandExecutor.execute(Command.newCommand("wallet_information"), key),
                "Expected 'Test message' for wallet information command.");
    }

    @Test
    void testWalletInvestmentInformationCommand() throws InvalidCryptoCoinException {
        when(userAccountsDatabase.getLoggedAccount(account)).thenReturn(mockedAccount);
        when(mockedAccount.walletInvestmentInformation(cryptoCoinsDatabase)).thenReturn("Test message");

        assertEquals("Test message",
                commandExecutor.execute(Command.newCommand("wallet_investment_information"), key)
                , "Expected 'Test message' for wallet investment information command.");
    }
}