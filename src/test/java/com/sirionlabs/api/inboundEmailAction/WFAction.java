package com.sirionlabs.api.inboundEmailAction;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.email.TestEmailAction;

import java.util.HashMap;

public class WFAction extends TestAPIBase {

    public static String getNewApiPath(String entityTypeId, String entityId) {
        return "/inboundactionemail/provideButtons/"+entityTypeId+"/"+entityId+"";
    }


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public static HashMap<String, String> getHeadersWithBadAuth() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "2324");
        return headers;
    }

    public static APIResponse getWFActionAPI(String apiPath, HashMap<String, String> headers) {
        APIResponse response = executor.get(apiPath, headers).getResponse();
        return response;
    }
}