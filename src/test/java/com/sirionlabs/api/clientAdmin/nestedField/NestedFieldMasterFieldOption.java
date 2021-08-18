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

public class NestedFieldMasterFieldOption extends TestAPIBase {

    public static String getApiPath(int masterFieldId) {
        return "/nestedfield/masterFieldOption/?masterFieldId=" + masterFieldId;
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");

        return headers;
    }

    public static APIResponse getMasterFieldOptionResponse(int masterFieldId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.post(getApiPath(masterFieldId), getHeaders(), null).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static List<Map<String, String>> getAllOptionsOfField(String masterFieldOptionResponse) {
        List<Map<String, String>> allOptionsOfField = new ArrayList<>();

        JSONArray optionsArr = new JSONObject(masterFieldOptionResponse).getJSONArray("masterFieldOption");

        for (int i = 0; i < optionsArr.length(); i++) {
            JSONObject jsonObj = optionsArr.getJSONObject(i);

            Map<String, String> optionMap = new HashMap<>();
            optionMap.put("name", jsonObj.getString("name"));
            optionMap.put("id", String.valueOf(jsonObj.getInt("id")));

            allOptionsOfField.add(optionMap);
        }

        return allOptionsOfField;
    }
}