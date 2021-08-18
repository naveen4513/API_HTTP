package com.sirionlabs.api.clientAdmin.UserAdmin;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class ImpersonationGetUsersAPI extends TestAPIBase {
    private static String apiPath = "/impersonation/getUsers";

    public static String getApiPath() {
        return apiPath;
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("Accept-Language", "en-IN,en-US;q=0.9,en-GB;q=0.8,en;q=0.7");
        return headers;
    }

    public static String getAllImpersonationUsersListResponseBody() {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;
        AdminHelper adminHelper=new AdminHelper();
        adminHelper.loginWithClientAdminUser();
        String response= executor.get(getApiPath(), getHeaders()).getResponse().getResponseBody();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        return response;
    }
}
