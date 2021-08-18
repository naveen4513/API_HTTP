package com.sirionlabs.api.todo.revamp;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class TodoStatusWiseCount extends TestAPIBase{

    public static String getApiPath(String occurrence, int entityTypeId) {

        return  "/pending-actions/"+occurrence+"/"+entityTypeId+"";
    }


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");

        return headers;
    }

    public static APIResponse getStatusWiseCount(String apiPath, HashMap<String, String> headers) {
        APIResponse response = executor.get(apiPath, headers).getResponse();
        return response;
    }

    public static HashMap<String, String> getHeadersWithInvalidAuth() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "e123");
        return headers;
    }
}