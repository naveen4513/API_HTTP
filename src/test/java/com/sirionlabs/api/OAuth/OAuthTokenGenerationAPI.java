package com.sirionlabs.api.OAuth;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import java.util.HashMap;

public class OAuthTokenGenerationAPI extends TestAPIBase {
   private final String queryString = "/oauth/client/token";
    private HashMap<String, String> getHeader() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "en-IN,en-US;q=0.9,en-GB;q=0.8,en;q=0.7");
        return headers;
    }
    public APIResponse getToken(String payload) {
        return executor.postWithoutMandatoryHeaders(queryString, getHeader(), payload).getResponse();
    }
}
