package crypto.wallet.manager.api;

import crypto.wallet.manager.exceptions.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiTest {

    private static final String API_ENDPOINT_SCHEME = "http";
    private static final String API_ENDPOINT_HOST = "rest.coinapi.io";
    private static final String API_ENDPOINT_PATH = "/v1/assets";
    private static final String API_KEY = "BAB0267C-B700-4857-AB51-4C9CEDCE8065";

    private Api api;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        api = new Api(mockHttpClient);
    }

    @Test
    void testSendRequest() throws URISyntaxException, IOException, InterruptedException {
        URI expectedUri = new URI(API_ENDPOINT_SCHEME, API_ENDPOINT_HOST, API_ENDPOINT_PATH, null);
        HttpRequest expectedRequest = HttpRequest.newBuilder()
                .uri(expectedUri)
                .header("X-CoinAPI-Key", API_KEY)
                .header("Accept", "application/json")
                .build();

        String responseBody = "Mocked response";
        HttpResponse mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(responseBody);

        when(mockHttpClient.send(Mockito.eq(expectedRequest), Mockito.eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockResponse);

        HttpResponse<String> result = api.sendRequest();

        assertEquals(responseBody, result.body(), "API response body should match the expected mocked response body");
    }

    @Test
    void testSendRequestWithIOException() throws IOException, InterruptedException {
        when(mockHttpClient.send(Mockito.any(), Mockito.eq(HttpResponse.BodyHandlers.ofString())))
                .thenThrow(IOException.class);

        assertThrows(ApiException.class, () -> api.sendRequest(),
                "Sending API request with IOException should throw ApiException");
    }
}