package com.sirionlabs.api.workflowButtons;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class WorkflowButtonCreate extends TestAPIBase {

    public static String getApiPath() {
        return "/workflowbuttons/v1/create";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");

        return headers;
    }

    public static String getPayload(String buttonName, String color, String description, Boolean active) {
        return getPayload(buttonName, color, description, active, 328);
    }

    public static String getPayload(String buttonName, String color, String description, Boolean active, int entityTypeId) {
        String buttonNamePayload = buttonName != null ? "\"name\": {\"values\": \"" + buttonName + "\"}" : "\"name\": {\"values\": null}";
        String colorPayload = color != null ? "\"color\": {\"values\": \"" + color + "\"}" : "\"color\": {\"values\": null}";

        return "{\"body\": {\"data\": {" + buttonNamePayload + "," + colorPayload + ",\"description\": {\"values\": \"" + description +
                "\"},\"active\": {\"values\": " + active + "},\"entityTypeId\": {\"values\": " + entityTypeId + "}}}}";
    }

    public static APIResponse getCreateResponse(String apiPath, HashMap<String, String> headers, String payload) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.post(apiPath, headers, payload).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}