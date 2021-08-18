package com.sirionlabs.api.delegation;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class DelegationCreateForm extends TestAPIBase {

    public static String getApiPath() {
        return "/delegation/create-form/v3";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static APIResponse getCreateFormV3Response() {
        return executor.get(getApiPath(), getHeaders()).getResponse();
    }
}