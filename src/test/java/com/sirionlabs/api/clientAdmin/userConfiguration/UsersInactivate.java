package com.sirionlabs.api.clientAdmin.userConfiguration;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class UsersInactivate extends TestAPIBase {

    public static String getAPIPath(int userID){
        return "/tblusers/inactivate/" + userID;
    }
    public static HashMap<String, String> getHeaders(){
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }
    public static int hitUsersInactivateAPI(int userID){
        return executor.get(getAPIPath(userID), getHeaders()).getResponse().getResponseCode();
    }
}
