package com.sirionlabs.api.clientAdmin.userConfiguration;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class UsersActivate extends TestAPIBase {

    public static String getAPIPath(int userId) {
        return "/tblusers/activate/" + userId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static int hitUsersActivateAPI(int userId) {
        return executor.get(getAPIPath(userId), getHeaders()).getResponse().getResponseCode();
    }
}