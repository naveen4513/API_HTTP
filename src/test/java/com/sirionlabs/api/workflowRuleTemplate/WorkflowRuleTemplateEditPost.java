package com.sirionlabs.api.workflowRuleTemplate;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class WorkflowRuleTemplateEditPost extends TestAPIBase {

    public static String getApiPath() {
        return "/workflowruletemplate/v1/edit";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");

        return headers;
    }

    public static String getPayload(int workflowRuleTemplateId, String ruleTemplateName, String ruleTemplateJson, Boolean active, int entityTypeId) {
        String ruleTemplateNamePayload = (ruleTemplateName == null) ? null : ("\"" + ruleTemplateName + "\"");
        String ruleTemplateJsonPayload = (ruleTemplateJson == null) ? null : ("\"" + ruleTemplateJson.replace("\"", "\\\"") + "\"");

        return "{\"body\": {\"data\": {\"entityType\": {\"name\": \"entityType\",\"id\": 12480,\"values\": {\"id\": " + entityTypeId +
                "}},\"workflowRuleTemplateName\": {\"id\": 12479,\"name\": \"\",\"values\": " + ruleTemplateNamePayload +
                "},\"workflowRuleTemplateJson\": {\"name\": \"workflowRuleTemplateJson\",\"id\": 12481,\"values\": " + ruleTemplateJsonPayload +
                "},\"active\": {\"name\": \"active\",\"id\": 12482,\"values\": " + active + "},\"entityTypeId\": {\"name\": \"entityTypeId\",\"values\": " +
                "331,\"multiEntitySupport\": false},\"id\": {\"name\": \"id\",\"values\": " + workflowRuleTemplateId + "}}}}";
    }

    public static APIResponse getEditPostResponse(String payload) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.post(getApiPath(), getHeaders(), payload).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}