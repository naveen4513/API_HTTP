package com.sirionlabs.api.inboundEmailAction;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import java.util.HashMap;

public class InboundEmailAction extends TestAPIBase{

    public static String getApiPath() {

        return  "/workflow-next-task-execution/execute";
    }


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        return headers;
    }


    public static APIResponse postInboundEmailActionAPI(String apiPath, HashMap<String, String> headers, String payload) {
        APIResponse response = executor.post(apiPath, headers, payload).getResponse();

        return response;
    }


    public static HashMap<String, String> getHeadersWithInvalidAuth(String emailAuthToken) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "a");

        return headers;
    }
}