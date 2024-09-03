package crypto.wallet.manager.database;

import crypto.wallet.manager.crypto.CryptoCoin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CryptoCoinsDatabaseTest {

    private CryptoCoinsDatabase cryptoCoinsDatabase;

    @BeforeEach
    void setUp() {
        cryptoCoinsDatabase = new CryptoCoinsDatabase();
    }

    @Test
    void updateCryptoCoinSet_validSet_shouldUpdateSet() {
        Set<CryptoCoin> newSet = createValidCryptoCoinSet();
        cryptoCoinsDatabase.updateCryptoCoinSet(newSet);

        assertEquals(newSet, cryptoCoinsDatabase.getCryptoCoinSet(),
                "Updating with a valid set should update the CryptoCoin set");
    }

    @Test
    void updateCryptoCoinSet_nullSet_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> cryptoCoinsDatabase.updateCryptoCoinSet(null),
                "Updating with a null set should throw IllegalArgumentException");
    }

    @Test
    void findCryptoCoinByOfferingCode_existingCode_shouldReturnOptionalCryptoCoin() {
        Set<CryptoCoin> newSet = createValidCryptoCoinSet();
        cryptoCoinsDatabase.updateCryptoCoinSet(newSet);

        Optional<CryptoCoin> foundCoin = cryptoCoinsDatabase.findCryptoCoinByOfferingCode("BTC");

        assertTrue(foundCoin.isPresent(), "Optional should contain a CryptoCoin");
        assertEquals("BTC", foundCoin.get().offeringCode(), "Found CryptoCoin should have offering code 'BTC'");
    }

    @Test
    void findCryptoCoinByOfferingCode_nonExistingCode_shouldReturnEmptyOptional() {
        Set<CryptoCoin> newSet = createValidCryptoCoinSet();
        cryptoCoinsDatabase.updateCryptoCoinSet(newSet);

        Optional<CryptoCoin> foundCoin = cryptoCoinsDatabase.findCryptoCoinByOfferingCode("XYZ");

        assertTrue(foundCoin.isEmpty(), "Optional should be empty for non-existing CryptoCoin");
    }

    @Test
    void listOfferings_withCryptoCoins_shouldReturnFormattedString() {
        Set<CryptoCoin> newSet = createValidCryptoCoinSet();
        cryptoCoinsDatabase.updateCryptoCoinSet(newSet);

        String offerings = cryptoCoinsDatabase.listOfferings();

        String expectedOutput = "Available cryptos:" +
                System.lineSeparator() +
                "Bitcoin (BTC) - 49000,00 US dollars" +
                System.lineSeparator() +
                "Ethereum (ETH) - 3000,00 US dollars";
        assertEquals(expectedOutput, offerings, "Listed offerings should match the expected formatted string");
    }

    @Test
    void listOfferings_emptyCryptoCoins_shouldReturnEmptyString() {
        String offerings = cryptoCoinsDatabase.listOfferings();
        assertEquals("Available cryptos:" + System.lineSeparator(), offerings,
                "Listed offerings for an empty CryptoCoinsDatabase should be an empty string");
    }

    private Set<CryptoCoin> createValidCryptoCoinSet() {
        Set<CryptoCoin> cryptoCoins = new HashSet<>();
        cryptoCoins.add(new CryptoCoin("BTC", "Bitcoin", 49000.0, 1));
        cryptoCoins.add(new CryptoCoin("ETH", "Ethereum", 3000.0, 1));
        return cryptoCoins;
    }
}
