package crypto.wallet.manager.account;

import crypto.wallet.manager.crypto.CryptoCoin;
import crypto.wallet.manager.database.CryptoCoinsDatabase;
import crypto.wallet.manager.exceptions.InsufficientBalanceException;
import crypto.wallet.manager.exceptions.InvalidCryptoCoinException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Account implements BaseAccount {

    public static final double FRESH_ACCOUNT_BALANCE = 0.0;

    private final String username;
    private final String password;
    private double amountAvailable;

    // offeringCode, amountOfCoin, priceBoughtAt
    private final Map<String, Map<Double, Double>> cryptoCoins;

    public Account(String username, String password) {
        this.username = username;
        this.password = hashPassword(password);
        this.cryptoCoins = new HashMap<>();
        this.amountAvailable = FRESH_ACCOUNT_BALANCE;
    }

    @Override
    public boolean passwordMatch(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password cannot be null");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, this.password);
    }

    @Override
    public void deposit(double amount) throws IllegalArgumentException {
        if (!isAmountValid(amount)) {
            throw new IllegalArgumentException("amount cannot be a negative number or 0");
        }

        amountAvailable += amount;
    }

    private Map<Double, Double> getOrCreateCryptoCoinEntry(String offeringCode) {
        return cryptoCoins.computeIfAbsent(offeringCode, k -> new HashMap<>());
    }

    @Override
    public void buyCryptoCoin(double amount, String offeringCode, CryptoCoinsDatabase cryptoCoinsDatabase)
            throws InvalidCryptoCoinException, InsufficientBalanceException {
        if (!isAmountValid(amount)) {
            throw new IllegalArgumentException("Amount cannot be a negative number or 0");
        }

        if (offeringCode == null) {
            throw new IllegalArgumentException("Offering code cannot be null");
        }

        CryptoCoin cryptoCoin = cryptoCoinsDatabase.findCryptoCoinByOfferingCode(offeringCode)
                .orElseThrow(() -> new InvalidCryptoCoinException("Crypto coin not found: " + offeringCode));

        double cost = amount * cryptoCoin.priceUSD();

        if (cost > amountAvailable) {
            throw new InsufficientBalanceException("Not enough available amount to pay for the transaction.");
        }

        Map<Double, Double> cryptoCoinEntry = getOrCreateCryptoCoinEntry(offeringCode);
        cryptoCoinEntry.put(amount, cryptoCoin.priceUSD());
        amountAvailable -= cost;
    }

    @Override
    public void sellCryptoCoin(String offeringCode, CryptoCoinsDatabase cryptoCoinsDatabase)
            throws InvalidCryptoCoinException {
        if (offeringCode == null) {
            throw new IllegalArgumentException("offeringCode cannot be null");
        }

        CryptoCoin cryptoCoin = cryptoCoinsDatabase.findCryptoCoinByOfferingCode(offeringCode)
                .orElseThrow(() -> new InvalidCryptoCoinException(
                        "Crypto coin with offering code not found: " + offeringCode));

        Map<Double, Double> cryptoCoinEntry = getOrCreateCryptoCoinEntry(offeringCode);
        double amountOfCoin = calculateTotalAmountOfCoins(cryptoCoinEntry.keySet());
        cryptoCoins.remove(offeringCode);
        deposit(cryptoCoin.priceUSD() * amountOfCoin);
    }

    @Override
    public String walletInformation() {
        StringBuilder result = new StringBuilder(
                String.format("Amount available: %.2f\nCurrent investments:\n", amountAvailable));

        cryptoCoins.forEach((offeringCode, coinAmount) ->
                result.append(String.format(
                        "%s - %.2f coins\n", offeringCode, calculateTotalAmountOfCoins(coinAmount.keySet()))));

        return result.toString();
    }

    @Override
    public String walletInvestmentInformation(CryptoCoinsDatabase cryptoCoinsDatabase)
            throws InvalidCryptoCoinException {
        if (cryptoCoinsDatabase == null) {
            throw new IllegalArgumentException("CryptoCoinsDatabase cannot be null");
        }

        Map<String, String> investmentData = calculateInvestmentData(cryptoCoinsDatabase);

        if (investmentData.isEmpty()) {
            return "Currently there aren't any investments.";
        }

        return formatInvestmentData(investmentData);
    }

    private Map<String, String> calculateInvestmentData(CryptoCoinsDatabase cryptoCoinsDatabase)
            throws InvalidCryptoCoinException {
        Map<String, String> investmentData = new HashMap<>();

        for (var entry : cryptoCoins.entrySet()) {
            String offeringCode = entry.getKey();
            CryptoCoin cryptoCoin = cryptoCoinsDatabase.findCryptoCoinByOfferingCode(offeringCode)
                    .orElseThrow(() -> new InvalidCryptoCoinException("Crypto coin not found: " + offeringCode));

            Map<Double, Double> coinAmountData = entry.getValue();
            double amountInvested = calculateTotalInvestment(coinAmountData);
            double amountOfCoin = calculateTotalAmountOfCoins(coinAmountData.keySet());
            double priceMargin = amountOfCoin * cryptoCoin.priceUSD() - amountInvested;

            String coinInformation = String.format("%s - %s%.2f\n",
                    offeringCode, (priceMargin >= FRESH_ACCOUNT_BALANCE) ? "+" : "", priceMargin);

            investmentData.put(offeringCode, coinInformation);
        }

        return investmentData;
    }

    private String formatInvestmentData(Map<String, String> investmentData) {
        StringBuilder result = new StringBuilder();
        investmentData.values().forEach(result::append);
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account that = (Account) o;

        return username.equals(that.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    private double calculateTotalAmountOfCoins(Set<Double> cryptoCoinAmount) {
        return cryptoCoinAmount.stream().mapToDouble(Double::doubleValue).sum();
    }

    private double calculateTotalInvestment(Map<Double, Double> cryptoCoinInvestmentData) {
        return cryptoCoinInvestmentData.entrySet().stream()
                .mapToDouble(entry -> entry.getKey() * entry.getValue())
                .sum();
    }

    private String hashPassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    public String getUsername() {
        return username;
    }

    public double getAmountAvailable() {
        return amountAvailable;
    }

    public Map<String, Map<Double, Double>> getCryptoCoins() {
        return Collections.unmodifiableMap(cryptoCoins);
    }

    public boolean isAmountValid(double amount) {
        return amount > FRESH_ACCOUNT_BALANCE;
    }
}
