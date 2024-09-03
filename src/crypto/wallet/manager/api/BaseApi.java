package crypto.wallet.manager.api;

import java.net.http.HttpResponse;

public interface BaseApi {
    HttpResponse<String> sendRequest();
}
