package com.sirionlabs.api.todo;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class TodoPendingApproval extends TestAPIBase {

    public static String getApiPath(int entityTypeId, String period, int start, int limit) {
        return "/api/v1/todo/pending/approval/" + entityTypeId + "?period=" + period + "&start=" + start + "&limit=" + limit;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static APIResponse getPendingApprovalResponse(String apiPath, HashMap<String, String> headers) {
        return executor.get(apiPath, headers).getResponse();
    }
}