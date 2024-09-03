package crypto.wallet.manager.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import crypto.wallet.manager.crypto.CryptoCoin;
import crypto.wallet.manager.database.CryptoCoinsDatabase;
import crypto.wallet.manager.exceptions.RequestFailedException;
import crypto.wallet.manager.exceptions.ResponseFailedException;

import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.Set;

public class ApiRunnable implements Runnable {
    private static final String RESPONSE_NULL_MESSAGE = "Couldn't get a response from the API service";
    private final Gson gson;
    private final Api api;
    private final CryptoCoinsDatabase cryptoCoinsDatabase;

    public ApiRunnable(Api api, CryptoCoinsDatabase cryptoCoinsDatabase) {
        this.api = api;
        this.cryptoCoinsDatabase = cryptoCoinsDatabase;
        gson = new Gson();
    }

    @Override
    public void run() {
        try {
            HttpResponse<String> response = api.sendRequest();
            handleResponse(response);
        } catch (Exception e) {
            throw new RequestFailedException("Request to API failed", e);
        }
    }

    private void handleResponse(HttpResponse<String> response) {
        if (response != null) {
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                Type type = new TypeToken<Set<CryptoCoin>>() {
                }.getType();
                Set<CryptoCoin> data = gson.fromJson(response.body(), type);
                cryptoCoinsDatabase.updateCryptoCoinSet(data);
            } else {
                throw new ResponseFailedException("Response status was different from OK");
            }
        } else {
            throw new ResponseFailedException(RESPONSE_NULL_MESSAGE);
        }
    }
}
