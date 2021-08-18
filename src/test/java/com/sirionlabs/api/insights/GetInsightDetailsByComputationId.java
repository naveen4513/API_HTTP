package com.sirionlabs.api.insights;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class GetInsightDetailsByComputationId extends TestAPIBase {

    public static String getApiPath(int listId, int insightComputationId) {
        return "/insights/list/" + listId + "/" + insightComputationId + "/getInsightDetailsByComputationId";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getPayload(String entityTypeId, String offset, String size, String orderByColumnName, String orderDirection, String filterJson,
                                    String selectedColumns) {
        return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName +
                "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":" + filterJson + "},\"selectedColumns\": " + selectedColumns + "}";
    }

    public static APIResponse getInsightDetailsResponse(int listId, int insightComputationId, String payload) {
        return executor.post(getApiPath(listId, insightComputationId), getHeaders(), payload).getResponse();
    }
}