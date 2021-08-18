package com.sirionlabs.api.timeLine;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;


import java.util.HashMap;

public class ActionHistory extends TestAPIBase {

    public static String getApiPath(int entityTypeId,int entityId) {
        return "/actionhistory/v1/" + entityTypeId + "/" + entityId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getContentTypeAsJsonOnlyHeader();
    }

    public static APIResponse getActionHistoryShowResponse(int entityTypeId,int entityId) {

        APIResponse response = executor.get(getApiPath(entityTypeId,entityId), getHeaders()).getResponse();

        return response;
    }
}