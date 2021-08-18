package com.sirionlabs.api.workflowRoleGroupFlowDown;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class WorkflowRoleGroupFlowDownCreate extends TestAPIBase {

    public static String getApiPath() {
        return "/rolegroupFlowdown/v1/create";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");

        return headers;
    }

    public static String getPayload(String[] parentEntityTypeIdArr, String[] childEntityTypeIdArr, String[] roleGroupIdArr, String[] clientIdArr, String[] deletedArr) {
        String payload = "[";

        for (int i = 0; i < parentEntityTypeIdArr.length; i++) {
            String subPayload = "{\"deleted\": " + deletedArr[i].trim() + ",\"childEntityTypeId\": " + childEntityTypeIdArr[i].trim() +
                    ",\"parentEntityTypeId\": " + parentEntityTypeIdArr[i].trim() + ",\"roleGroupId\": " + roleGroupIdArr[i].trim() + ",\"clientId\": " +
                    clientIdArr[i].trim() + "}";
            payload = payload.concat(subPayload) + ",";
        }

        payload = payload.substring(0, payload.length() - 1).concat("]");

        return payload;
    }

    public static String getPayloadFlowDownRoleGroup(String[] parentEntityTypeIdArr, String[] childEntityTypeIdArr, String[] roleGroupIdArr, String[] flowEnabled, String[] deletedArr) {
        String payload = "[";

        for (int i = 0; i < parentEntityTypeIdArr.length; i++) {
            String subPayload = "{\"deleted\": " + deletedArr[i].trim() + ",\"childEntityTypeId\": " + childEntityTypeIdArr[i].trim() +
                    ",\"parentEntityTypeId\": " + parentEntityTypeIdArr[i].trim() + ",\"roleGroupId\": " + roleGroupIdArr[i].trim() + ",\"flowdownEnabled\": " +
                    flowEnabled[i].trim() + "}";
            payload = payload.concat(subPayload) + ",";
        }

        payload = payload.substring(0, payload.length() - 1).concat("]");

        return payload;
    }

    public static APIResponse getCreateResponse(String payload) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.post(getApiPath(), getHeaders(), payload).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}