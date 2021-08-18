package com.sirionlabs.api.workflowRuleTemplate;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class WorkflowRuleTemplateDelete extends TestAPIBase {

    public static String getApiPath() {
        return "/workflowruletemplate/v1/delete";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");

        return headers;
    }

    public static String getPayload(int ruleTemplateId) {
        return "{\"body\": {\"data\": {\"id\": {\"name\": \"id\",\"values\": " + ruleTemplateId + "}}}}";
    }

    public static APIResponse getDeleteResponse(String payload) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.post(getApiPath(), getHeaders(), payload).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}