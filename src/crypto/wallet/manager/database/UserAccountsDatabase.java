package crypto.wallet.manager.database;

import crypto.wallet.manager.account.Account;
import crypto.wallet.manager.exceptions.AccountAlreadyExistsException;
import crypto.wallet.manager.exceptions.AccountDoesNotExistException;
import crypto.wallet.manager.exceptions.AccountIsAlreadyLoggedInException;
import crypto.wallet.manager.exceptions.AccountNotLoggedInException;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class UserAccountsDatabase implements AutoCloseable {
    private static volatile UserAccountsDatabase instance;
    private final Set<Account> allAccounts;
    private final Set<Account> currentlyUsedAccounts;
    private final String accountsPath;

    private UserAccountsDatabase(String accountsPath) {
        allAccounts = new HashSet<>();
        currentlyUsedAccounts = new HashSet<>();
        this.accountsPath = accountsPath;
    }

    public static UserAccountsDatabase getInstance(String accountsPath) {
        if (instance == null) {
            synchronized (UserAccountsDatabase.class) {
                instance = new UserAccountsDatabase(accountsPath);
            }
        }

        return instance;
    }

    public void createAccount(Account account) throws AccountAlreadyExistsException {
        if (allAccounts.contains(account)) {
            throw new AccountAlreadyExistsException("Account with such name already exists");
        }

        allAccounts.add(account);
        writeAccountsToFile();
    }

    public Account login(String username, String password)
            throws AccountDoesNotExistException, AccountIsAlreadyLoggedInException {
        for (Account account : allAccounts) {
            if (account.passwordMatch(password) && account.getUsername().equals(username)) {
                if (currentlyUsedAccounts.contains(account)) {
                    throw new AccountIsAlreadyLoggedInException("Account is already in use.");
                }

                currentlyUsedAccounts.add(account);
                return account;
            }
        }

        throw new AccountDoesNotExistException("No account with these credentials exists in the database");
    }

    public void logOut(Account account) {
        currentlyUsedAccounts.remove(account);
    }

    public String getAccountsPath() {
        return accountsPath;
    }

    private void readAccountsFromFile(Set<Account> accountSet) {
        Path pathOfAccounts = Path.of(accountsPath);
        ensureFileExists(pathOfAccounts);

        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(pathOfAccounts))) {
            Object accountObject;
            while ((accountObject = objectInputStream.readObject()) != null) {
                Account account = (Account) accountObject;
                allAccounts.add(account);
            }
        } catch (EOFException e) {
            // try with resources was throwing EOFException
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("The accounts file does not exist", e);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("A problem occurred while reading accounts from a file", e);
        }
    }

    private void writeAccountsToFile() {
        Path pathOfAccounts = Path.of(accountsPath);

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(pathOfAccounts))) {
            for (Account account : allAccounts) {
                objectOutputStream.writeObject(account);
            }
        } catch (IOException e) {
            throw new IllegalStateException("A problem occurred while writing accounts to a file", e);
        }
    }

    public void loadDatabase() {
        Path pathOfAccounts = Path.of(accountsPath);
        ensureFileExists(pathOfAccounts);
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(pathOfAccounts))) {
            Account account;
            while (true) {
                try {
                    account = (Account) ois.readObject();
                    allAccounts.add(account);
                } catch (EOFException e) {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Accounts file not found, starting with an empty database.");
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("A problem occurred while reading accounts from file", e);
        }
    }

    public Account getLoggedAccount(Account account) {
        if (currentlyUsedAccounts.contains(account)) {
            return account;
        }

        throw new AccountNotLoggedInException("Account is not logged in. Please log in");
    }

    private void ensureFileExists(Path filePath) {
        try {
            if (Files.notExists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create file: " + filePath, e);
        }
    }

    public Set<Account> getAllAccounts() {
        return allAccounts;
    }

    public Set<Account> getCurrentlyUsedAccounts() {
        return currentlyUsedAccounts;
    }

    @Override
    public void close() {
        writeAccountsToFile();
    }
}
