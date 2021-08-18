package com.sirionlabs.api.clientAdmin.accessCriteria;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class AccessCriteriaCreate {

    public static String getApiPath() {
        return "/access-criteria/create";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = ApiHeaders.getDefaultHeadersForClientAdminAPIs();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        return headers;
    }
}