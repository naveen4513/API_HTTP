package com.sirionlabs.api.CheckEligibleForAdhocMeeting;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class CheckEligibleForAdhocMeeting extends TestAPIBase {
    public static String getApiPath(int entityID) {
        return "/tblgovernancebodychild/checkEligibleForAdhocMeeting/"+entityID;
    }
    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getOnlyHeaderWithAuthorization();
    }
    public static APIResponse getResponse(int entityId)
    {
        APIResponse response = executor.getAPIWithoutMandatoryDefaultHeaders(getApiPath(entityId), getHeaders()).getResponse();
        return response;
    }
}
