package com.sirionlabs.api.docusignService;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class Send extends TestAPIBase {

    public static String getNewApiPath(String serviceType) {

        return "/" + serviceType + "/send";
    }


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");

        return headers;
    }


    public static APIResponse postSendAPI(String apiPath, HashMap<String, String> headers, String payload) {
        APIResponse response = executor.post(apiPath, headers, payload).getResponse();

        return response;
    }
}