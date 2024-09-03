package crypto.wallet.manager.api;

import crypto.wallet.manager.exceptions.ApiException;
import crypto.wallet.manager.exceptions.RequestFailedException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public record Api(HttpClient httpClient) implements BaseApi {
    public static final String API_ENDPOINT_SCHEME = "https";

    public static final String API_ENDPOINT_HOST = "rest.coinapi.io";

    public static final String API_ENDPOINT_PATH = "/v1/assets";

    public static final String API_KEY = "c5024942-da07-45db-9eec-75531a19abe3";

    public HttpResponse<String> sendRequest() {
        HttpResponse<String> httpResponse;

        try {
            URI uri = new URI(API_ENDPOINT_SCHEME, API_ENDPOINT_HOST, API_ENDPOINT_PATH, null);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("X-CoinAPI-Key", API_KEY)
                    .header("Accept", "application/json")
                    .build();

            httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException e) {
            throw new RequestFailedException("Could not build a request", e);
        } catch (IOException | InterruptedException e) {
            throw new ApiException("Error inside the api", e);
        }

        return httpResponse;
    }
}

