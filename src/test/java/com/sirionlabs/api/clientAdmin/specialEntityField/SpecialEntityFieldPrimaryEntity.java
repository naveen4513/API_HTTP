package com.sirionlabs.api.clientAdmin.specialEntityField;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpecialEntityFieldPrimaryEntity extends TestAPIBase {

    public static String getApiPath(int entityTypeId) {
        return "/specialentityfield/v1/primaryEntity/" + entityTypeId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static APIResponse getCreateFormResponse(int primaryEntityTypeId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(getApiPath(primaryEntityTypeId), getHeaders()).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static List<Map<String, String>> getAllPrimaryEntityCustomFieldsMap(String response) {
        List<Map<String, String>> allCustomFieldsMap = new ArrayList<>();

        JSONArray jsonArr = new JSONArray(response);
        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.has("name") && !jsonObj.isNull("name")) {
                Map<String, String> fieldMap = new HashMap<>();
                fieldMap.put("id", String.valueOf(jsonObj.getInt("id")));
                fieldMap.put("name", jsonObj.getString("name"));

                allCustomFieldsMap.add(fieldMap);
            }
        }

        return allCustomFieldsMap;
    }
}