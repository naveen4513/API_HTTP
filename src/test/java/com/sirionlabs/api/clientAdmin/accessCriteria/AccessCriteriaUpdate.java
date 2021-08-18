package com.sirionlabs.api.clientAdmin.accessCriteria;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class AccessCriteriaUpdate {

    public static String getApiPath() {
        return "/access-criteria/update";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = ApiHeaders.getDefaultHeadersForClientAdminAPIs();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        return headers;
    }
}