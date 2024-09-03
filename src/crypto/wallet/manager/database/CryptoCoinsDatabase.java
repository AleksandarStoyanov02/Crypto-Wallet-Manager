package crypto.wallet.manager.database;

import crypto.wallet.manager.crypto.CryptoCoin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CryptoCoinsDatabase {
    private static final int COINS_CAPACITY = 100;

    private static Set<CryptoCoin> cryptoCoinSet;

    public CryptoCoinsDatabase() {
        cryptoCoinSet = new HashSet<>();
    }

    public Set<CryptoCoin> getCryptoCoinSet() {
        return Collections.unmodifiableSet(cryptoCoinSet);
    }

    public void updateCryptoCoinSet(Set<CryptoCoin> newSet) {
        if (newSet == null) {
            throw new IllegalArgumentException("The new set is not created correctly.");
        }

        cryptoCoinSet = newSet.stream()
                .filter(cryptoCoin -> cryptoCoin.isCrypto() == 1)
                .limit(COINS_CAPACITY)
                .collect(Collectors.toSet());
    }

    public Optional<CryptoCoin> findCryptoCoinByOfferingCode(String offeringCode) {
        return cryptoCoinSet.stream()
                .filter(coin -> coin.offeringCode().equals(offeringCode))
                .findFirst();
    }

    public String listOfferings() {
        return cryptoCoinSet.stream()
                .map(coin -> String.format("%s (%s) - %.2f US dollars",
                        coin.name(), coin.offeringCode(), coin.priceUSD()))
                .collect(Collectors.joining(System.lineSeparator(), "Available cryptos:" + System.lineSeparator(), ""));
    }

}
