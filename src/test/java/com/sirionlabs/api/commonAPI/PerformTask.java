package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class PerformTask extends TestAPIBase {

    public static String getApiPath(String entityName) {
        String searchUrl = ConfigureConstantFields.getSearchUrlForEntity(entityName);
        return "/" + searchUrl + "/review/performTask";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static int cancelReview(String entityName, String payload) {
        return executor.post(getApiPath(entityName), getHeaders(), payload).getResponse().getResponseCode();
    }
}