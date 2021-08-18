package com.sirionlabs.api.presignature;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class FindMappedTags extends TestAPIBase {

    public static String getApiPath(int cdrId) {
        return "/tblcdr/findMappedTags?contractDraftRequestId=" + cdrId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getFindMappedTagsResponse(int cdrId, String payload) {
        return executor.post(getApiPath(cdrId), getHeaders(), payload).getResponse().getResponseBody();
    }
}