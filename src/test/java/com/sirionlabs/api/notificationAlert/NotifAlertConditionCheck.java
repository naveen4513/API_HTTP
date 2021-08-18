package com.sirionlabs.api.notificationAlert;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.email.TestEmailAction;

import java.util.HashMap;

public class NotifAlertConditionCheck extends TestAPIBase {

    public static String getNewApiPath(String entityId, String ruleId, String currentDate) {

        return "/auditLog/evaluateStatus?entityId="+entityId+"&ruleId="+ruleId+"&currentDate="+currentDate+"";
    }


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", TestEmailAction.getAuthorizationKey());

        return headers;
    }


    public static APIResponse postNotifAlertConditionCheckAPI(String apiPath, HashMap<String, String> headers, String payload) {
        APIResponse response = executor.post(apiPath, headers, payload).getResponse();

        return response;
    }
}