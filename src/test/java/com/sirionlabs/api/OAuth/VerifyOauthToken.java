package com.sirionlabs.api.OAuth;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class VerifyOauthToken extends TestAPIBase {
    String queryString;
    public APIResponse getVerifyToken(String accessToken) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;
        AdminHelper adminHelper=new AdminHelper();
        adminHelper.loginWithClientAdminUser();
        queryString="/auth/api/v1/token/oauth/verify?token="+accessToken;
        APIResponse response=  executor.get("http://192.168.2.242:8086",queryString,new HashMap<>()).getResponse();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}
