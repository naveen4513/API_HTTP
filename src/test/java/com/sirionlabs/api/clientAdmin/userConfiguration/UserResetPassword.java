package com.sirionlabs.api.clientAdmin.userConfiguration;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class UserResetPassword extends TestAPIBase {
    public static String getAPIPath(int userID){
        return "/tblusers/resetPassword/" + userID;
    }
    public static HashMap<String, String> getHeaders(){
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }
    public static int hitResetPasswordAPI(int userID){
        return executor.get(getAPIPath(userID), getHeaders()).getResponse().getResponseCode();
    }
}
