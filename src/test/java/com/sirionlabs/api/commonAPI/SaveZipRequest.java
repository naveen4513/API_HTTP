package com.sirionlabs.api.commonAPI;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class SaveZipRequest extends TestAPIBase {

    public static String getApiPathType2(int entityTypeId, int recordId) {
        return "/saveZipRequest/2/" + entityTypeId + "/" + recordId;
    }

    public static String getApiPathType3(int entityTypeId, int recordId) {
        return "/v1/saveZipRequest/3/" + entityTypeId + "/" + recordId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getSaveZipRequestResponseType2(int entityTypeId, int recordId, String payload) {
        return executor.post(getApiPathType2(entityTypeId, recordId), getHeaders(), payload).getResponse().getResponseBody();
    }

    public static String getSaveZipRequestResponseType3(int entityTypeId, int recordId, String payload) {
        return executor.post(getApiPathType3(entityTypeId, recordId), getHeaders(), payload).getResponse().getResponseBody();
    }
}