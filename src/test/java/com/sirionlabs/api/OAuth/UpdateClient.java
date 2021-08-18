package com.sirionlabs.api.OAuth;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class UpdateClient extends TestAPIBase {
    String queryString = "/oauth/client/update/";

    public HashMap<String, String> getHeader() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "en-IN,en-US;q=0.9,en-GB;q=0.8,en;q=0.7");
        return headers;
    }

    public APIResponse updateClientAPI(int userId,String payload) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;
        AdminHelper adminHelper=new AdminHelper();
        adminHelper.loginWithClientAdminUser();
        queryString+=userId;
        APIResponse response=executor.post(queryString, getHeader(), payload).getResponse();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}
