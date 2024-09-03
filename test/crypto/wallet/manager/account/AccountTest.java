package crypto.wallet.manager.account;

import crypto.wallet.manager.database.CryptoCoinsDatabase;
import crypto.wallet.manager.exceptions.InsufficientBalanceException;
import crypto.wallet.manager.exceptions.InvalidCryptoCoinException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


import crypto.wallet.manager.crypto.CryptoCoin;

import java.util.HashSet;
import java.util.Set;

class AccountTest {

    private static final String VALID_USERNAME = "testUser";
    private static final String VALID_PASSWORD = "testPassword";

    private Account testAccount;
    private CryptoCoinsDatabase cryptoCoinsDatabase;

    @BeforeEach
    void setUp() {
        testAccount = new Account(VALID_USERNAME, VALID_PASSWORD);
        cryptoCoinsDatabase = new CryptoCoinsDatabase();

        Set<CryptoCoin> initialCryptoCoins = initializeCryptoCoins();
        cryptoCoinsDatabase.updateCryptoCoinSet(initialCryptoCoins);

        testAccount.deposit(100000.0);
    }

    private Set<CryptoCoin> initializeCryptoCoins() {
        Set<CryptoCoin> cryptoCoins = new HashSet<>();
        cryptoCoins.add(new CryptoCoin("BTC", "Bitcoin", 49000.0, 1));
        cryptoCoins.add(new CryptoCoin("ETH", "Ethereum", 3000.0, 1));
        return cryptoCoins;
    }

    @Test
    void buyCryptoCoin_validInput_shouldSucceed() {
        try {
            testAccount.buyCryptoCoin(10.0, "ETH", cryptoCoinsDatabase);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        assertEquals(70000.0, testAccount.getAmountAvailable(), "Account balance should be updated");
        assertTrue(testAccount.getCryptoCoins().containsKey("ETH"), "Account should contain ETH crypto coins");
    }

    @Test
    void buyCryptoCoin_insufficientBalance_shouldThrowException() {
        assertThrows(InsufficientBalanceException.class,
                () -> testAccount.buyCryptoCoin(5000.0, "BTC", cryptoCoinsDatabase),
                "Buying crypto coins with insufficient balance should throw InsufficientBalanceException");
    }

    @Test
    void sellCryptoCoin_validInput_shouldSucceed() throws InsufficientBalanceException, InvalidCryptoCoinException {
        testAccount.buyCryptoCoin(1, "ETH", cryptoCoinsDatabase);
        try {
            testAccount.sellCryptoCoin("ETH", cryptoCoinsDatabase);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        assertEquals(VALID_USERNAME, testAccount.getUsername(), "Account username should remain unchanged");
        assertEquals(100000.0, testAccount.getAmountAvailable(), "Account balance should remain unchanged");
        assertFalse(testAccount.getCryptoCoins().containsKey("ETH"), "Account should not contain ETH crypto coins");
    }

    @Test
    void sellCryptoCoin_cryptoCoinNotFound_shouldThrowException() {
        assertThrows(InvalidCryptoCoinException.class,
                () -> testAccount.sellCryptoCoin("XRP", cryptoCoinsDatabase),
                "Selling a non-existent crypto coin should throw InvalidCryptoCoinException");
    }

    @Test
    void walletInformation_shouldReturnCorrectInfo() throws InsufficientBalanceException, InvalidCryptoCoinException {
        testAccount.buyCryptoCoin(1, "BTC", cryptoCoinsDatabase);
        testAccount.buyCryptoCoin(5, "ETH", cryptoCoinsDatabase);

        String expectedInfo = "Amount available: 36000,00\n" +
                "Current investments:\n" +
                "BTC - 1,00 coins\n" +
                "ETH - 5,00 coins\n";
        assertEquals(expectedInfo, testAccount.walletInformation(), "Wallet information should be correct");
    }

    @Test
    void walletInvestmentInformation_shouldReturnNoInvestments() throws InvalidCryptoCoinException {
        String expectedInfo = "Currently there aren't any investments.";
        assertEquals(expectedInfo, testAccount.walletInvestmentInformation(cryptoCoinsDatabase),
                "Wallet investment information should indicate no investments");
    }

    @Test
    void buyCryptoCoin_negativeAmount_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> testAccount.buyCryptoCoin(-100.0, "BTC", cryptoCoinsDatabase),
                "Buying crypto coins with a negative amount should throw IllegalArgumentException");
    }

    @Test
    void buyCryptoCoin_zeroAmount_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> testAccount.buyCryptoCoin(0.0, "BTC", cryptoCoinsDatabase),
                "Buying crypto coins with zero amount should throw IllegalArgumentException");
    }

    @Test
    void buyCryptoCoin_nullOfferingCode_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> testAccount.buyCryptoCoin(10.0, null, cryptoCoinsDatabase),
                "Buying crypto coins with a null offering code should throw IllegalArgumentException");
    }

    @Test
    void buyCryptoCoin_invalidOfferingCode_shouldThrowException() {
        assertThrows(InvalidCryptoCoinException.class,
                () -> testAccount.buyCryptoCoin(10.0, "XYZ", cryptoCoinsDatabase),
                "Buying crypto coins with an invalid offering code should throw InvalidCryptoCoinException");
    }

    @Test
    void sellCryptoCoin_nullOfferingCode_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> testAccount.sellCryptoCoin(null, cryptoCoinsDatabase),
                "Selling crypto coins with a null offering code should throw IllegalArgumentException");
    }

    @Test
    void sellCryptoCoin_invalidOfferingCode_shouldThrowException() {
        assertThrows(InvalidCryptoCoinException.class,
                () -> testAccount.sellCryptoCoin("XYZ", cryptoCoinsDatabase),
                "Selling crypto coins with an invalid offering code should throw InvalidCryptoCoinException");
    }

    @Test
    void walletInvestmentInformation_nullCryptoCoinsDatabase_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> testAccount.walletInvestmentInformation(null),
                "Retrieving wallet investment information with null CryptoCoinsDatabase should throw IllegalArgumentException");
    }

    @Test
    void walletInformation_noInvestments_shouldReturnCorrectInfo() {
        String expectedInfo = "Amount available: 100000,00\n" +
                "Current investments:\n";
        assertEquals(expectedInfo, testAccount.walletInformation(),
                "Wallet information should be correct when there are no investments");
    }

    @Test
    void walletInvestmentInformation_noInvestments_shouldReturnCorrectInfo() throws InvalidCryptoCoinException {
        String expectedInfo = "Currently there aren't any investments.";
        assertEquals(expectedInfo, testAccount.walletInvestmentInformation(cryptoCoinsDatabase),
                "Wallet investment information should indicate no investments");
    }

    @Test
    void walletInvestmentInformation_multipleCoins_shouldReturnCorrectInfo() throws InsufficientBalanceException, InvalidCryptoCoinException {
        testAccount.buyCryptoCoin(1, "BTC", cryptoCoinsDatabase);
        testAccount.buyCryptoCoin(3, "ETH", cryptoCoinsDatabase);

        String expectedInfo = "BTC - +0,00\nETH - +0,00\n";
        assertEquals(expectedInfo, testAccount.walletInvestmentInformation(cryptoCoinsDatabase),
                "Wallet investment information should be correct for multiple coins");
    }

    @Test
    void walletInvestmentInformation_negativeInvestment_shouldReturnCorrectInfo() throws InsufficientBalanceException, InvalidCryptoCoinException {
        testAccount.buyCryptoCoin(1, "BTC", cryptoCoinsDatabase);
        testAccount.buyCryptoCoin(5, "ETH", cryptoCoinsDatabase);
        testAccount.sellCryptoCoin("BTC", cryptoCoinsDatabase);

        String expectedInfo = "ETH - +0,00\n";
        assertEquals(expectedInfo, testAccount.walletInvestmentInformation(cryptoCoinsDatabase),
                "Wallet investment information should be correct for negative investment");
    }

    @Test
    void walletInvestmentInformation_invalidCryptoCoinsDatabase_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> testAccount.walletInvestmentInformation(null),
                "Retrieving wallet investment information with an invalid CryptoCoinsDatabase should throw IllegalArgumentException");
    }

    @Test
    void walletInvestmentInformation_emptyCryptoCoinsDatabase_shouldReturnNoInvestments() throws InvalidCryptoCoinException {
        CryptoCoinsDatabase emptyDatabase = new CryptoCoinsDatabase();
        String expectedInfo = "Currently there aren't any investments.";
        assertEquals(expectedInfo, testAccount.walletInvestmentInformation(emptyDatabase),
                "Wallet investment information should indicate no investments for an empty CryptoCoinsDatabase");
    }
}