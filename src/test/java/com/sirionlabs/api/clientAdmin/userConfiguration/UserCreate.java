package com.sirionlabs.api.clientAdmin.userConfiguration;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class UserCreate {

    public static String getApiPath() {
        return "/tblusers";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = ApiHeaders.getDefaultHeadersForClientAdminAPIs();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        return headers;
    }
}