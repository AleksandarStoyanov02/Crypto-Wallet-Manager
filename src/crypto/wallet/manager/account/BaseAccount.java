package crypto.wallet.manager.account;

import crypto.wallet.manager.database.CryptoCoinsDatabase;
import crypto.wallet.manager.exceptions.InsufficientBalanceException;
import crypto.wallet.manager.exceptions.InvalidCryptoCoinException;

import java.io.Serializable;

public interface BaseAccount extends Serializable {

    boolean passwordMatch(String password);

    void deposit(double amount);

    void buyCryptoCoin(double amount, String offeringCode, CryptoCoinsDatabase cryptoCoinsDatabase)
            throws InvalidCryptoCoinException, InsufficientBalanceException;

    void sellCryptoCoin(String offeringCode, CryptoCoinsDatabase cryptoCoinsDatabase)
            throws InvalidCryptoCoinException;

    String walletInformation();

    String walletInvestmentInformation(CryptoCoinsDatabase cryptoCoinsDatabase)
            throws InvalidCryptoCoinException;
}
