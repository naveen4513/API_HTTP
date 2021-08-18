package com.sirionlabs.api.clientAdmin.nestedField;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class NestedFieldUpdate extends TestAPIBase {

    public static String getApiPath() {
        return "/nestedfield/update";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getEmailDefaultHeaders();
    }

    public static String getUpdatePayload(int masterFieldId, String masterFieldType, int childFieldId, int childFieldOptionId) {
        return "{\"fieldId\":\"" + masterFieldId + "\",\"type\":\"" + masterFieldType + "\",\"childs\":[{\"deleted\":false,\"added\":false,\"fieldId\":\"" +
                childFieldId + "\",\"selectedOptions\":[{\"id\":\"" + childFieldOptionId + "\"}]}]}";
    }

    public static int getUpdateResponseCode(String payload) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.post(getApiPath(), getHeaders(), payload).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response.getResponseCode();
    }
}