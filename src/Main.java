import crypto.wallet.manager.api.Api;
import crypto.wallet.manager.server.CryptoWalletManagerServer;

import java.io.IOException;
import java.net.http.HttpClient;

public class Main {
    public static void main(String[] args) {
        try {
            CryptoWalletManagerServer cryptoWalletManagerServer = CryptoWalletManagerServer.getInstance();
            Api api = new Api(HttpClient.newBuilder().build());
            cryptoWalletManagerServer.start(api);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}