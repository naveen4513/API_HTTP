package com.sirionlabs.api.clientAdmin.nestedField;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NestedFieldFormData extends TestAPIBase {

    public static String getApiPath(int entityTypeId) {
        return "/nestedfield/formData/?entityTypeId=" + entityTypeId;
    }

    public static String getV1ApiPath(int entityTypeId) {
        return "/nestedfield/formData/v1?entityTypeId=" + entityTypeId;
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");

        return headers;
    }

    public static APIResponse getFormDataResponse(int entityTypeId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.post(getApiPath(entityTypeId), getHeaders(), null).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static APIResponse getFormDataV1Response(int entityTypeId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(getV1ApiPath(entityTypeId), getHeaders()).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static List<Map<String, String>> getAllMasterFields(String formDataResponse) {
        List<Map<String, String>> allFieldsOfType = new ArrayList<>();

        JSONArray masterFieldsArr = new JSONObject(formDataResponse).getJSONArray("masterFields");

        for (int i = 0; i < masterFieldsArr.length(); i++) {
            JSONObject jsonObj = masterFieldsArr.getJSONObject(i);

            Map<String, String> fieldMap = new HashMap<>();

            fieldMap.put("name", jsonObj.getString("name"));
            fieldMap.put("id", String.valueOf(jsonObj.getInt("fieldId")));
            fieldMap.put("type", jsonObj.getString("type"));

            allFieldsOfType.add(fieldMap);
        }

        return allFieldsOfType;
    }

    public static Map<Integer, String> getAllChildFields(String formDataResponse) {
        Map<Integer, String> allChildFields = new HashMap<>();

        JSONArray childFieldsArr = new JSONObject(formDataResponse).getJSONArray("childFields");

        for (int i = 0; i < childFieldsArr.length(); i++) {
            JSONObject jsonObj = childFieldsArr.getJSONObject(i);

            allChildFields.put(jsonObj.getInt("fieldId"), jsonObj.getString("name"));
        }

        return allChildFields;
    }
}