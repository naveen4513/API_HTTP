package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class fetchAllIdsAPI extends TestAPIBase {
    public static String getAPIPath() {
        return "/autoextraction/bulk/fetchAllIds/";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getPayload() {
        return "{\"listId\":432,\"filterMap\":{},\"contractId\":\"\",\"relationId\":\"\",\"vendorId\":\"\"," +
                "\"entityIds\":[]}";
    }

    public static APIResponse fetchAllIdResponse(String apiPath, HashMap<String, String> headers, String payload) {
        return executor.post(apiPath, headers, payload).getResponse();
    }

}
