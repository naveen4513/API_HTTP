package com.sirionlabs.api.OAuth;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class GetClientSecretAPI extends TestAPIBase {
    String queryString;

    public APIResponse getClientSecretAPI(String clientId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;
        AdminHelper adminHelper=new AdminHelper();
        adminHelper.loginWithClientAdminUser();
        queryString = "/auth/oauth/client/secret/" + clientId;
        APIResponse response= executor.get( queryString, new HashMap<>()).getResponse();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

}
