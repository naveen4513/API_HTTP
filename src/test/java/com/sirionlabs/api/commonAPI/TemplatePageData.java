package com.sirionlabs.api.commonAPI;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class TemplatePageData extends TestAPIBase {

    public static String getApiPath(int contractTemplateId) {
        return "/tblcontracttemplate/templatePageData/" + contractTemplateId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getTemplatePageDataResponse(int contractTemplateId) {
        return executor.get(getApiPath(contractTemplateId), getHeaders()).getResponse().getResponseBody();
    }
}