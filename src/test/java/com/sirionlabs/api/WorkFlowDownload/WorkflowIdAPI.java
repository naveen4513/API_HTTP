package com.sirionlabs.api.WorkFlowDownload;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class WorkflowIdAPI extends TestAPIBase {
    public static String getApiPath() {
        return "/workflow/workflowId?shortCodeId=";
    }
    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Language","en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7");
        return headers;
    }
    public static APIResponse getResponse(String shortCodeId)
    {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();
        String apiPath=getApiPath()+shortCodeId;
        APIResponse response = executor.get(apiPath, getHeaders()).getResponse();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}
