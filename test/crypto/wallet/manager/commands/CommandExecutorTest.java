package crypto.wallet.manager.commands;

import crypto.wallet.manager.account.Account;
import crypto.wallet.manager.crypto.CryptoCoin;
import crypto.wallet.manager.database.CryptoCoinsDatabase;
import crypto.wallet.manager.database.UserAccountsDatabase;
import crypto.wallet.manager.exceptions.AccountAlreadyExistsException;
import crypto.wallet.manager.exceptions.AccountDoesNotExistException;
import crypto.wallet.manager.exceptions.AccountIsAlreadyLoggedInException;
import crypto.wallet.manager.exceptions.InsufficientBalanceException;
import crypto.wallet.manager.exceptions.InvalidCryptoCoinException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.channels.SelectionKey;
import java.util.Optional;

import static crypto.wallet.manager.commands.CommandErrorMessageType.CRYPTO_COIN_DOES_NOT_EXIST;
import static crypto.wallet.manager.commands.CommandErrorMessageType.DISCONNECTED;
import static crypto.wallet.manager.commands.CommandErrorMessageType.INVALID_INPUT_ARGUMENTS;
import static crypto.wallet.manager.commands.CommandErrorMessageType.LOGIN_SUCCESSFUL;
import static crypto.wallet.manager.commands.CommandErrorMessageType.MUST_LOGIN;
import static crypto.wallet.manager.commands.CommandErrorMessageType.REGISTER_SUCCESSFUL;
import static crypto.wallet.manager.commands.CommandErrorMessageType.SUCCESSFUL_OPERATION;
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
    private static UserAccountsDatabase userAccountsDatabase = mock(UserAccountsDatabase.class);
    private static CryptoCoinsDatabase cryptoCoinsDatabase = mock(CryptoCoinsDatabase.class);
    private static SelectionKey key = mock(SelectionKey.class);
    private static CommandExecutor commandExecutor;
    private Account mockedAccount = mock(Account.class);
    private Account account;

    @BeforeEach
    void setUp() {
        commandExecutor = CommandExecutor.getInstance(userAccountsDatabase, cryptoCoinsDatabase);
        account = new Account("user", "1");
        key.attach(account);
    }

    @Test
    public void testLoginAccountDoesNotExist() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        key.attach(null);
        when(userAccountsDatabase.login(anyString(), anyString())).thenThrow(AccountDoesNotExistException.class);

        assertEquals("Wrong username or password.", commandExecutor
                .execute(Command.newCommand("login user1 1"), key));
    }

    @Test
    public void tesLoginTooManyArguments() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        key.attach(null);
        when(userAccountsDatabase.login("user", "1")).thenThrow(AccountDoesNotExistException.class);

        assertEquals("Invalid command", commandExecutor
                .execute(Command.newCommand("login user 1 1"), key));
    }

    @Test
    public void testExecuteLoginAccountAlreadyLoggedIn() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        assertEquals("You are already logged in.", commandExecutor
                .execute(Command.newCommand("login user 1"), key));
    }

    @Test
    void testLogin() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException, AccountAlreadyExistsException {
        Account loggedInAccount = new Account("user", "1");
        when(userAccountsDatabase.login(anyString(), anyString())).thenReturn(loggedInAccount);

        String result = commandExecutor.execute(Command.newCommand("login user 1"), key);

        assertEquals("Login successful", result);
        assertEquals(loggedInAccount, key.attachment());
    }

    @Test
    void testUnknownCommand() {
        Command unknownCommand = new Command(CommandType.UNKNOWN, null);

        String result = commandExecutor.execute(unknownCommand, key);

        assertEquals(UNKNOWN_COMMAND_MESSAGE.getMessage(), result);
    }

    @Test
    void testBuyCrypto() throws InsufficientBalanceException, InvalidCryptoCoinException {
        when(userAccountsDatabase.getLoggedAccount(account)).thenReturn(mockedAccount);
        doNothing().when(mockedAccount).buyCryptoCoin(anyDouble(), anyString(), eq(cryptoCoinsDatabase));

        assertEquals("Transaction completed",
                commandExecutor.execute(Command.newCommand("buy_crypto BTC 50"), key));
    }

    @Test
    void testSellCryptoCryptoCoinDoesNotExist() {
        Command sellCryptoCommand = new Command(CommandType.SELL_CRYPTO, new String[]{"BTC"});
        Account mockAccount = new Account("username", "password");

        when(key.attachment()).thenReturn(mockAccount);
        when(userAccountsDatabase.getLoggedAccount(mockAccount)).thenReturn(mockAccount); // Fix here
        when(cryptoCoinsDatabase.findCryptoCoinByOfferingCode("BTC")).thenReturn(Optional.empty());

        String result = commandExecutor.execute(sellCryptoCommand, key);

        assertEquals(CRYPTO_COIN_DOES_NOT_EXIST.getMessage(), result);
    }

    @Test
    void testSellCryptoNotLoggedIn() {
        key.attach(null);

        assertEquals("Log in first or create a new account if you don't have one.",
                commandExecutor.execute(Command.newCommand("sell_crypto"), key));
    }

    @Test
    void testSellCryptoTooManyArguments() throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        assertEquals(INVALID_INPUT_ARGUMENTS,
                commandExecutor.execute(Command.newCommand("sell_crypto 1 A B 4"), key));
    }

    @Test
    public void testSellCryptoSuccessful() throws InvalidCryptoCoinException {
        when(userAccountsDatabase.getLoggedAccount(account)).thenReturn(mockedAccount);
        doNothing().when(mockedAccount).sellCryptoCoin(anyString(), eq(cryptoCoinsDatabase));

        assertEquals("Transaction completed",
                commandExecutor.execute(Command.newCommand("sell_crypto BTC"), key));
    }

    @Test
    void testDisconnect() {
        Command disconnectCommand = new Command(CommandType.DISCONNECT, null);
        Account mockAccount = new Account("username", "password");

        when(key.attachment()).thenReturn(mockAccount);

        String result = commandExecutor.execute(disconnectCommand, key);

        assertEquals(DISCONNECTED.getMessage(), result);
    }

    @Test
    void testShutdownCommand() {
        Command shutdownCommand = new Command(SHUTDOWN, null);

        String result = commandExecutor.execute(shutdownCommand, key);

        assertEquals(SHUTDOWN.toString(), result);
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

        assertEquals(REGISTER_SUCCESSFUL.getMessage(), result);
        verify(userAccountsDatabase).createAccount(any());
    }

    @Test
    public void testExecuteListOfferingsLoggedIn() {
        when(cryptoCoinsDatabase.listOfferings()).thenReturn("Test output");

        assertEquals("Test output",
                commandExecutor.execute(Command.newCommand("list_cryptos"), key));
    }

    @Test
    void testWalletInformationCommand() {
        when(userAccountsDatabase.getLoggedAccount(account)).thenReturn(mockedAccount);
        when(mockedAccount.walletInformation()).thenReturn("Test message");

        assertEquals("Test message",
                commandExecutor.execute(Command.newCommand("wallet_information"), key));
    }

    @Test
    void testWalletInvestmentInformationCommand() throws InvalidCryptoCoinException {
        when(userAccountsDatabase.getLoggedAccount(account)).thenReturn(mockedAccount);
        when(mockedAccount.walletInvestmentInformation(cryptoCoinsDatabase)).thenReturn("Test message");

        assertEquals("Test message",
                commandExecutor.execute(Command.newCommand("wallet_investment_information"), key));
    }
}