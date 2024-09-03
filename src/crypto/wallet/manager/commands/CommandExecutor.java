package crypto.wallet.manager.commands;

import crypto.wallet.manager.account.Account;
import crypto.wallet.manager.database.CryptoCoinsDatabase;
import crypto.wallet.manager.database.UserAccountsDatabase;
import crypto.wallet.manager.exceptions.AccountAlreadyExistsException;
import crypto.wallet.manager.exceptions.AccountDoesNotExistException;
import crypto.wallet.manager.exceptions.AccountIsAlreadyLoggedInException;
import crypto.wallet.manager.exceptions.InsufficientBalanceException;
import crypto.wallet.manager.exceptions.InvalidCryptoCoinException;

import java.nio.channels.SelectionKey;

import static crypto.wallet.manager.commands.Command.REQUIRED_ARGUMENTS_FOR_DEPOSIT_CRYPTO;
import static crypto.wallet.manager.commands.Command.REQUIRED_ARGUMENTS_FOR_LOGIN_REGISTER_BUY_CRYPTO;
import static crypto.wallet.manager.commands.Command.REQUIRED_ARGUMENTS_FOR_SELLING_CRYPTO;
import static crypto.wallet.manager.commands.CommandErrorMessageType.ACCOUNT_EXISTS;
import static crypto.wallet.manager.commands.CommandErrorMessageType.ALREADY_LOGGED_IN;
import static crypto.wallet.manager.commands.CommandErrorMessageType.ALREADY_LOGGED_IN_REGISTER_MESSAGE;
import static crypto.wallet.manager.commands.CommandErrorMessageType.CRYPTO_COIN_DOES_NOT_EXIST;
import static crypto.wallet.manager.commands.CommandErrorMessageType.DISCONNECTED;
import static crypto.wallet.manager.commands.CommandErrorMessageType.INSUFFICIENT_AMOUNT;
import static crypto.wallet.manager.commands.CommandErrorMessageType.INVALID_FORMAT_AMOUNT;
import static crypto.wallet.manager.commands.CommandErrorMessageType.INVALID_INPUT_ARGUMENTS;
import static crypto.wallet.manager.commands.CommandErrorMessageType.INVALID_LOGIN;
import static crypto.wallet.manager.commands.CommandErrorMessageType.LOGIN_SUCCESSFUL;
import static crypto.wallet.manager.commands.CommandErrorMessageType.MUST_LOGIN;
import static crypto.wallet.manager.commands.CommandErrorMessageType.NEGATIVE_AMOUNT;
import static crypto.wallet.manager.commands.CommandErrorMessageType.PROBLEM_WHILE_LOGGING_IN;
import static crypto.wallet.manager.commands.CommandErrorMessageType.REGISTER_SUCCESSFUL;
import static crypto.wallet.manager.commands.CommandErrorMessageType.SERVER_SIDE_ERROR;
import static crypto.wallet.manager.commands.CommandErrorMessageType.SUCCESSFUL_OPERATION;
import static crypto.wallet.manager.commands.CommandErrorMessageType.UNKNOWN_COMMAND_MESSAGE;
import static crypto.wallet.manager.commands.CommandType.SHUTDOWN;

public class CommandExecutor {
    private static CommandExecutor instance;
    private final UserAccountsDatabase accounts;
    private final CryptoCoinsDatabase cryptoCoinsDatabase;

    private CommandExecutor(UserAccountsDatabase accounts, CryptoCoinsDatabase cryptoCoinsDatabase) {
        this.accounts = accounts;
        this.cryptoCoinsDatabase = cryptoCoinsDatabase;
    }

    public static CommandExecutor getInstance(UserAccountsDatabase accounts, CryptoCoinsDatabase cryptoCoinsDatabase) {
        if (instance == null) {
            instance = new CommandExecutor(accounts, cryptoCoinsDatabase);
        }

        return instance;
    }

    public String execute(Command command, SelectionKey key) {
        return switch (command.type()) {
            case LOGIN -> login(command.arguments(), key);
            case REGISTER -> register(command.arguments(), key);
            case DEPOSIT -> deposit(command.arguments(), key);
            case LIST_CRYPTOS -> listCryptos(key);
            case BUY_CRYPTO -> buyCrypto(command.arguments(), key);
            case SELL_CRYPTO -> sellCrypto(command.arguments(), key);
            case WALLET_INFORMATION -> getWalletInformation(key);
            case WALLET_INVESTMENT_INFORMATION -> getWalletInvestmentInformation(key);
            case DISCONNECT -> disconnect(key);
            case HELP -> help();
            case SHUTDOWN -> SHUTDOWN.toString();
            default -> UNKNOWN_COMMAND_MESSAGE.getMessage();
        };
    }

    private String help() {
        return "Available commands: " + System.lineSeparator() +
                "login {name} {password}" + System.lineSeparator() +
                "register {name} {password}" + System.lineSeparator() +
                "deposit {amount}" + System.lineSeparator() +
                "list_cryptos" + System.lineSeparator() +
                "buy_crypto {id} {amount}" + System.lineSeparator() +
                "sell_crypto {id}" + System.lineSeparator() +
                "wallet_information" + System.lineSeparator() +
                "wallet_investment_information" + System.lineSeparator() +
                "disconnect" + System.lineSeparator();
    }

    private String disconnect(SelectionKey key) {
        Account account;

        if (key.attachment() == null) {
            return MUST_LOGIN.getMessage();
        } else {
            account = (Account) key.attachment();
        }

        accounts.logOut(account);
        return DISCONNECTED.getMessage();
    }

    private String login(String[] args, SelectionKey key) {
        if (key.attachment() != null) {
            return ALREADY_LOGGED_IN.getMessage();
        }

        if (args.length != REQUIRED_ARGUMENTS_FOR_LOGIN_REGISTER_BUY_CRYPTO) {
            return INVALID_INPUT_ARGUMENTS.getMessage();
        }

        String username = args[0];
        String password = args[1];

        try {
            Account account = accounts.login(username, password);
            key.attach(account);
        } catch (AccountDoesNotExistException e) {
            return INVALID_LOGIN.getMessage();
        } catch (AccountIsAlreadyLoggedInException e) {
            return ALREADY_LOGGED_IN.getMessage();
        } catch (IllegalArgumentException e) {
            return PROBLEM_WHILE_LOGGING_IN.getMessage();
        }

        return LOGIN_SUCCESSFUL.getMessage();
    }

    private String register(String[] args, SelectionKey key) {
        if (key.attachment() != null) {
            return ALREADY_LOGGED_IN_REGISTER_MESSAGE.getMessage();
        }

        if (args.length != REQUIRED_ARGUMENTS_FOR_LOGIN_REGISTER_BUY_CRYPTO) {
            return INVALID_INPUT_ARGUMENTS.getMessage();
        }

        String username = args[0];
        String password = args[1];

        try {
            Account account = new Account(username, password);
            accounts.createAccount(account);
        } catch (AccountAlreadyExistsException e) {
            return ACCOUNT_EXISTS.getMessage();
        }

        return REGISTER_SUCCESSFUL.getMessage();
    }

    private String deposit(String[] args, SelectionKey key) {
        Account account;

        if (key.attachment() == null) {
            return MUST_LOGIN.getMessage();
        } else {
            account = (Account) key.attachment();
        }

        if (args.length != REQUIRED_ARGUMENTS_FOR_DEPOSIT_CRYPTO) {
            return INVALID_INPUT_ARGUMENTS.getMessage();
        }

        try {
            double amount = Double.parseDouble(args[0]);
            accounts.getLoggedAccount(account).deposit(amount);
        } catch (NumberFormatException | NullPointerException e) {
            return INVALID_FORMAT_AMOUNT.getMessage();
        } catch (IllegalArgumentException e) {
            return NEGATIVE_AMOUNT.getMessage();
        }

        return SUCCESSFUL_OPERATION.getMessage();
    }

    private String listCryptos(SelectionKey key) {
        if (key.attachment() == null) {
            return MUST_LOGIN.getMessage();
        }

        return cryptoCoinsDatabase.listOfferings();
    }

    private String buyCrypto(String[] args, SelectionKey key) {
        Account account;

        if (key.attachment() == null) {
            return MUST_LOGIN.getMessage();
        } else {
            account = (Account) key.attachment();
        }

        if (args.length != REQUIRED_ARGUMENTS_FOR_LOGIN_REGISTER_BUY_CRYPTO) {
            return INVALID_INPUT_ARGUMENTS.getMessage();
        }

        try {
            String offeringCode = args[0].toUpperCase();
            double amount = Double.parseDouble(args[1]);
            accounts.getLoggedAccount(account).buyCryptoCoin(amount, offeringCode, cryptoCoinsDatabase);
        } catch (NumberFormatException | NullPointerException e) {
            return INVALID_FORMAT_AMOUNT.getMessage();
        } catch (IllegalArgumentException e) {
            return NEGATIVE_AMOUNT.getMessage();
        } catch (InsufficientBalanceException e) {
            return INSUFFICIENT_AMOUNT.getMessage();
        } catch (InvalidCryptoCoinException e) {
            return CRYPTO_COIN_DOES_NOT_EXIST.getMessage();
        }

        return SUCCESSFUL_OPERATION.getMessage();
    }

    private String sellCrypto(String[] args, SelectionKey key) {
        Account account;

        if (key.attachment() == null) {
            return MUST_LOGIN.getMessage();
        } else {
            account = (Account) key.attachment();
        }

        if (args.length != REQUIRED_ARGUMENTS_FOR_SELLING_CRYPTO) {
            return INVALID_INPUT_ARGUMENTS.getMessage();
        }

        String offeringCode = args[0];

        try {
            accounts.getLoggedAccount(account).sellCryptoCoin(offeringCode, cryptoCoinsDatabase);
        } catch (InvalidCryptoCoinException e) {
            return CRYPTO_COIN_DOES_NOT_EXIST.getMessage();
        }

        return SUCCESSFUL_OPERATION.getMessage();
    }

    private String getWalletInformation(SelectionKey key) {
        Account account;

        if (key.attachment() == null) {
            return MUST_LOGIN.getMessage();
        } else {
            account = (Account) key.attachment();
        }

        return accounts.getLoggedAccount(account).walletInformation();
    }

    private String getWalletInvestmentInformation(SelectionKey key) {
        Account account;

        if (key.attachment() == null) {
            return MUST_LOGIN.getMessage();
        } else {
            account = (Account) key.attachment();
        }

        try {
            return accounts.getLoggedAccount(account).walletInvestmentInformation(cryptoCoinsDatabase);
        } catch (InvalidCryptoCoinException e) {
            return SERVER_SIDE_ERROR.getMessage();
        }
    }

    public CryptoCoinsDatabase getCryptoCoinsDatabase() {
        return cryptoCoinsDatabase;
    }
}
