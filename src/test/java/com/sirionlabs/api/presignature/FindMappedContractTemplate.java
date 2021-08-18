package com.sirionlabs.api.presignature;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class FindMappedContractTemplate extends TestAPIBase {

    public static String getApiPath(int cdrId) {
        return "/tblcdr/findMappedContractTemplate?entityId=" + cdrId + "&entityTypeId=160";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getFindMappedContractTemplateResponse(int cdrId) {
        return executor.get(getApiPath(cdrId), getHeaders()).getResponse().getResponseBody();
    }
}