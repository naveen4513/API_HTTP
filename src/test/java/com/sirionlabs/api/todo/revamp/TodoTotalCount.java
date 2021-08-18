package com.sirionlabs.api.todo.revamp;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class TodoTotalCount extends TestAPIBase{

    public static String getApiPath(String occurrence) {

        return  "/pending-actions/"+occurrence+"/count";
    }


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        return headers;
    }


    public static APIResponse getTotalCount(String apiPath, HashMap<String, String> headers) {
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