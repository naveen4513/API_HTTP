package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.json.JSONObject;

import java.util.HashMap;

public class DeviationSummary extends TestAPIBase {

    public static String getApiPath(String entityName, int recordId, String documentFileId) {
        String searchUrl = ConfigureConstantFields.getSearchUrlForEntity(entityName);
        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        return "/" + searchUrl + "/deviationSummary/" + entityTypeId + "/" + recordId + "?documentFileId=" + documentFileId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getDeviationSummaryResponse(String entityName, int recordId, String documentFileId) {
        return executor.get(getApiPath(entityName, recordId, documentFileId), getHeaders()).getResponse().getResponseBody();
    }

    public static boolean isDeviationCompleted(String deviationSummaryResponse) {
        return new JSONObject(deviationSummaryResponse).getBoolean("deviationCompleted");
    }
}