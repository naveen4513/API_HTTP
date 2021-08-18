package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class GetAllFieldsAPI extends TestAPIBase {
    public static String getAPIPath() {
        return "/metadataautoextraction/getAllFields";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static APIResponse fetchAllMetadataFields(String apiPath,HashMap<String, String> headers) {
        return executor.get(apiPath,headers).getResponse();

    }
}
