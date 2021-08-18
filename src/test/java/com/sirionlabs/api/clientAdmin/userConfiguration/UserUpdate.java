package com.sirionlabs.api.clientAdmin.userConfiguration;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class UserUpdate extends TestAPIBase {

    public static String getApiPath() {
        return "/tblusers/update";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = ApiHeaders.getDefaultHeadersForClientAdminAPIs();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        return headers;
    }

    public static int hitUserUpdate(Map<String, String> params) {

        return executor.post(getApiPath(), getHeaders(), null, params).getResponse().getResponseCode();
    }
    public static HashMap<String, String> getUserAdminHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html, */*; q=0.01");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("Accept-Language","en-IN,en-US;q=0.9,en-GB;q=0.8,en;q=0.7");
        return headers;
    }
    public static int userUpdateForUserAdmin(Map<String, String> params,String hostUrl) throws UnsupportedEncodingException {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;
        new ClientSetupHelper().loginWithUserAdmin();
        int response=executor.postMultiPartFormData(hostUrl,getApiPath(), getUserAdminHeaders(),params).getResponse().getResponseCode();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}