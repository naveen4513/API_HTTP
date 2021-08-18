package com.sirionlabs.api.clientAdmin.nestedField;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NestedFieldEntityTypes extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(NestedFieldEntityTypes.class);

    public static String getApiPath() {
        return "/nestedfield/entityTypes";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");

        return headers;
    }

    public static APIResponse getEntityTypesResponse() {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.post(getApiPath(), getHeaders(), null).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static List<Integer> getAllEntityTypeIds(String nestedFieldsEntityTypeResponse) {
        List<Integer> allEntityTypeIds = new ArrayList<>();

        try {
            JSONArray jsonArr = new JSONArray(nestedFieldsEntityTypeResponse);

            for (int i = 0; i < jsonArr.length(); i++) {
                allEntityTypeIds.add(jsonArr.getJSONObject(i).getInt("id"));
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Entity Type Ids List from Response. " + e.getMessage());
            return null;
        }

        return allEntityTypeIds;
    }
}