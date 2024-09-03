package crypto.wallet.manager.database;

import crypto.wallet.manager.account.Account;
import crypto.wallet.manager.exceptions.AccountAlreadyExistsException;
import crypto.wallet.manager.exceptions.AccountDoesNotExistException;
import crypto.wallet.manager.exceptions.AccountIsAlreadyLoggedInException;
import crypto.wallet.manager.exceptions.AccountNotLoggedInException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountsDatabaseTest {

    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "testPassword";

    private static UserAccountsDatabase userAccountsDatabase;
    private static Account testAccount;
    @TempDir
    static Path tempDir;

    @BeforeAll
    static void createTestAccountAndSetUp() throws AccountAlreadyExistsException {
        userAccountsDatabase = UserAccountsDatabase.getInstance(tempDir.resolve("testAccounts.dat").toString());
        testAccount = new Account(TEST_USERNAME, TEST_PASSWORD);
        userAccountsDatabase.createAccount(testAccount);
    }

    @AfterEach
    void teardown() {
        userAccountsDatabase.logOut(testAccount);
    }

    @Test
    void createAccount_validAccount_shouldCreateAccount() throws AccountAlreadyExistsException {
        assertEquals(1, userAccountsDatabase.getAllAccounts().size(),
                "Creating a valid account should add it to the list of all accounts");
        assertTrue(userAccountsDatabase.getAllAccounts().contains(testAccount),
                "The list of all accounts should contain the created test account");
    }

    @Test
    void createAccount_duplicateAccount_shouldThrowException() throws AccountAlreadyExistsException {
        Account duplicateAccount = new Account(TEST_USERNAME, "differentPassword");

        assertThrows(AccountAlreadyExistsException.class,
                () -> userAccountsDatabase.createAccount(duplicateAccount),
                "Creating a duplicate account should throw AccountAlreadyExistsException");
    }

    @Test
    void login_validCredentials_shouldLoginAccount() throws AccountAlreadyExistsException,
            AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        Account loggedInAccount = userAccountsDatabase.login(TEST_USERNAME, TEST_PASSWORD);

        assertEquals(testAccount, loggedInAccount,
                "Logging in with valid credentials should return the correct account");
        assertTrue(userAccountsDatabase.getCurrentlyUsedAccounts().contains(testAccount),
                "The currently used accounts should contain the logged-in account");
    }

    @Test
    void login_invalidCredentials_shouldThrowException() throws AccountAlreadyExistsException {
        assertThrows(AccountDoesNotExistException.class,
                () -> userAccountsDatabase.login(TEST_USERNAME, "invalidPassword"),
                "Logging in with invalid credentials should throw AccountDoesNotExistException");
    }

    @Test
    void login_accountAlreadyLoggedIn_shouldThrowException() throws AccountAlreadyExistsException,
            AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        userAccountsDatabase.login(TEST_USERNAME, TEST_PASSWORD);

        assertThrows(AccountIsAlreadyLoggedInException.class,
                () -> userAccountsDatabase.login(TEST_USERNAME, TEST_PASSWORD),
                "Logging in with an already logged-in account should throw AccountIsAlreadyLoggedInException");
    }

    @Test
    void logout_validAccount_shouldLogoutAccount() throws AccountAlreadyExistsException,
            AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        userAccountsDatabase.login(TEST_USERNAME, TEST_PASSWORD);

        userAccountsDatabase.logOut(testAccount);

        assertFalse(userAccountsDatabase.getCurrentlyUsedAccounts().contains(testAccount),
                "Logging out a valid account should remove it from the currently used accounts");
    }

    @Test
    void getAccountsPath_shouldReturnCorrectPath() {
        assertEquals(tempDir.resolve("testAccounts.dat").toString(), userAccountsDatabase.getAccountsPath(),
                "getAccountsPath should return the correct path");
    }

    @Test
    void getLoggedAccount_validLoggedInAccount_shouldReturnAccount() throws AccountAlreadyExistsException,
            AccountDoesNotExistException, AccountIsAlreadyLoggedInException, AccountNotLoggedInException {
        userAccountsDatabase.login(TEST_USERNAME, TEST_PASSWORD);

        Account loggedAccount = userAccountsDatabase.getLoggedAccount(testAccount);

        assertEquals(testAccount, loggedAccount,
                "getLoggedAccount should return the correct logged account");
    }

    @Test
    void getLoggedAccount_accountNotLoggedIn_shouldThrowException() throws AccountAlreadyExistsException {
        assertThrows(AccountNotLoggedInException.class,
                () -> userAccountsDatabase.getLoggedAccount(testAccount),
                "Getting the logged account for an account that is not logged in should throw AccountNotLoggedInException");
    }

    @Test
    void testLoadingDatabase() {
        Account dummyAccount = new Account("dummyUser", "dummyPassword");
        assertDoesNotThrow(() -> {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(tempDir.resolve("testAccounts.dat")))) {
                objectOutputStream.writeObject(dummyAccount);
            }
        });

        UserAccountsDatabase userDatabase = UserAccountsDatabase.getInstance(tempDir.resolve("testAccounts.dat").toString());
        userDatabase.loadDatabase();
        assertTrue(userDatabase.getAllAccounts().contains(dummyAccount),
                "Loading the database should include the dummy account");
    }

    @Test
    void testCloseMethod() {
        assertDoesNotThrow(() -> {
            try (UserAccountsDatabase userDatabase = UserAccountsDatabase.getInstance(tempDir.resolve("testAccounts.dat").toString())) {
                // Just to test the autoCloseable interface
            }
        });

        assertTrue(Files.exists(tempDir.resolve("testAccounts.dat")),
                "The database file should still exist after closing");
    }
}