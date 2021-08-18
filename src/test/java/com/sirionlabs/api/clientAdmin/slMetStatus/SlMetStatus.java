package com.sirionlabs.api.clientAdmin.slMetStatus;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class SlMetStatus extends TestAPIBase {

    public static String getApiPath() {
        return "/slmetstatus/list";
    }


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("Content-Type", "application/json");

        return headers;
    }


    public static APIResponse getSlMetStatus() {

        APIResponse response = executor.get(getApiPath(), getHeaders()).getResponse();

        return response;
    }



}
