package com.sirionlabs.api.search;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class EntityId extends TestAPIBase {

    public static String getNewApiPath(int seqId, int entityTypeId ) {

        return "/tblcontracts/getEntityId/"+seqId+"?entityTypeId="+entityTypeId+"";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public static APIResponse getEntityTypeId(int seqId, int entityTypeId) {
        APIResponse response = executor.get(getNewApiPath(seqId, entityTypeId), getHeaders()).getResponse();

        return response;
    }
}