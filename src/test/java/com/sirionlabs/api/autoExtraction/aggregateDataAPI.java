package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.json.JSONObject;

import java.util.HashMap;

public class aggregateDataAPI extends TestAPIBase {

    public static String getAPIPath() {
        return "/autoextraction/aggregate-data";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
    public static String getPayload() {
        APIResponse fetchIdApiResponse = fetchAllIdsAPI.fetchAllIdResponse(fetchAllIdsAPI.getAPIPath(), fetchAllIdsAPI.getHeaders(), fetchAllIdsAPI.getPayload());
        String fetchIdApiResponseStr = fetchIdApiResponse.getResponseBody();
        JSONObject fetchIdResponse=new JSONObject(fetchIdApiResponseStr);
        String allEntityIds=fetchIdResponse.get("entityIds").toString();
        return "{\"docIds\":"+allEntityIds+"}";
    }

    public static APIResponse aggregateDataAPIResponse(String apiPath, HashMap<String, String> headers, String payload) {
        return executor.post(apiPath, headers, payload).getResponse();
    }
}
