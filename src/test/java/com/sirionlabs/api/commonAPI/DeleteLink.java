package com.sirionlabs.api.commonAPI;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class DeleteLink extends TestAPIBase {

    public static String getApiPath() {
        return "/linkentity/deletelink";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getDeleteLinkResponse(String payload) {
        return executor.post(getApiPath(), getHeaders(), payload).getResponse().getResponseBody();
    }
}