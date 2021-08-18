package com.sirionlabs.api.workflowButtons;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class WorkflowButtonShow extends TestAPIBase {

    public static String getApiPath(int workflowButtonId) {
        return "/workflowbuttons/v1/show/" + workflowButtonId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static APIResponse getShowResponse(String apiPath, HashMap<String, String> headers) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(apiPath, headers).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}