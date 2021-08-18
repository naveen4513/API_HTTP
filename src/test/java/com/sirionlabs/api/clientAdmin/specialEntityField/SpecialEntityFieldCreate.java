package com.sirionlabs.api.clientAdmin.specialEntityField;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;
import java.util.Map;

public class SpecialEntityFieldCreate extends TestAPIBase {

    public static String getApiPath() {
        return "/specialentityfield/v1/create";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");

        return headers;
    }

    public static String getPayload(String primaryEntityCustomFieldName, String primaryEntityCustomFieldId, Integer primaryEntityTypeId, Integer secondaryEntityTypeId,
                                    Map<String, String> secondaryEntityFieldsMap, Boolean active) {
        String primaryEntityCustomFieldSubPayload = "{\"name\": \"primaryEntityCustomField\",\"id\": 12537,\"values\": {\"name\": \"" + primaryEntityCustomFieldName +
                "\",\"id\": " + primaryEntityCustomFieldId + "}}";

        String secondaryEntityFieldsSubPayload = null;

        if (secondaryEntityFieldsMap != null) {
            secondaryEntityFieldsSubPayload = "[";

            for (Map.Entry<String, String> entryMap : secondaryEntityFieldsMap.entrySet()) {
                secondaryEntityFieldsSubPayload = secondaryEntityFieldsSubPayload.concat("{\"name\": \"" + entryMap.getValue() + "\",\"id\": " + entryMap.getKey() + "},");
            }

            secondaryEntityFieldsSubPayload = secondaryEntityFieldsSubPayload.substring(0, secondaryEntityFieldsSubPayload.length() - 1).concat("]");
        }

        return "{\"body\": {\"data\": {\"primaryEntity\": {\"name\": \"primaryEntity\",\"id\": 12538,\"values\": {\"id\": " + primaryEntityTypeId +
                "}},\"secondaryEntity\": {\"name\": \"secondaryEntity\",\"id\": 12536,\"values\": {\"id\": " + secondaryEntityTypeId +
                "}},\"secondaryEntityFields\": {\"name\": \"secondaryEntityFields\",\"id\": 12535,\"values\": " + secondaryEntityFieldsSubPayload +
                "},\"primaryEntityCustomField\": " + primaryEntityCustomFieldSubPayload + ",\"active\": {\"name\": \"active\",\"id\": 12542,\"values\": " + active + "}}}}";
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