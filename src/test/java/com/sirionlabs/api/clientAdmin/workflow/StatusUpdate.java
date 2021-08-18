package com.sirionlabs.api.clientAdmin.workflow;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.Map;

import java.util.HashMap;

public class StatusUpdate extends TestAPIBase {

    public static String getApiPath() {
        return "/workflow/statusUpdate";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept-Encoding", "gzip, deflate");

        return headers;
    }

    public static boolean updateWorkflowStatus(Map<String, String> params) {
        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;

        try {
            AdminHelper adminHelper=new AdminHelper();
            adminHelper.loginWithClientAdminUser();
            int responseCode = executor.postMultiPartFormData(getApiPath(), getHeaders(), params).getResponse().getResponseCode();
            return responseCode == 302;
        } catch (Exception e) {
            return false;
        }
        finally {
            Check check=new Check();
            check.hitCheck(lastUserName,lastUserPassword);
        }
    }
}