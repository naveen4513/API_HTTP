package com.sirionlabs.api.OAuth;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class OAuthDeleteClient extends TestAPIBase {

    String queryString = "/oauth/client/delete";

    public HashMap<String, String> getHeader() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public APIResponse deleteClientAPI(String payload) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;
        AdminHelper adminHelper=new AdminHelper();
        adminHelper.loginWithClientAdminUser();
        APIResponse response= executor.post(queryString, getHeader(), payload).getResponse();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}
