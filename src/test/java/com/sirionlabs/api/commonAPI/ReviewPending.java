package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class ReviewPending extends TestAPIBase {

    public static String getApiPath(String entityName, int entityTypeId, int recordId) {
        String urlName = ConfigureConstantFields.getUrlNameForEntity(entityName);
        return "/" + urlName + "/reviewPending/" + entityTypeId + "/" + recordId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getReviewPendingResponse(String entityName, int entityTypeId, int recordId) {
        return executor.get(getApiPath(entityName, entityTypeId, recordId), getHeaders()).getResponse().getResponseBody();
    }
}