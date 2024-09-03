package crypto.wallet.manager.commands;

public enum CommandType {
    LOGIN,
    REGISTER,
    DEPOSIT,
    LIST_CRYPTOS,
    BUY_CRYPTO,
    SELL_CRYPTO,
    WALLET_INFORMATION,
    WALLET_INVESTMENT_INFORMATION,
    DISCONNECT,
    HELP,
    SHUTDOWN,
    UNKNOWN;

    public static CommandType fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return UNKNOWN;
        }
    }
}