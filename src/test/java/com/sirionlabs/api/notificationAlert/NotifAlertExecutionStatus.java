package com.sirionlabs.api.notificationAlert;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.email.TestEmailAction;

import java.util.HashMap;

public class NotifAlertExecutionStatus extends TestAPIBase {

    public static String getNewApiPath(String entityId, String ruleId) {
        return "/auditLog/status?entityId="+entityId+"&ruleId="+ruleId+"";
    }


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", TestEmailAction.getAuthorizationKey());
        return headers;
    }


    public static APIResponse getNotifAlertExecutionStatusAPI(String apiPath, HashMap<String, String> headers, String payload) {
        APIResponse response = executor.get(apiPath, headers).getResponse();
        return response;
    }
}