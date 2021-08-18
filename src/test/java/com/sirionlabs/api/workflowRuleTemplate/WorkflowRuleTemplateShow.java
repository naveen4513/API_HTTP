package com.sirionlabs.api.workflowRuleTemplate;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class WorkflowRuleTemplateShow extends TestAPIBase {

    public static String getApiPath(int workflowRuleTemplateId) {
        return "/workflowruletemplate/v1/show/" + workflowRuleTemplateId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static APIResponse getWorkflowRuleTemplateShowResponse(int workflowRuleTemplateId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(getApiPath(workflowRuleTemplateId), getHeaders()).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}