package com.sirionlabs.api.clientAdmin.specialEntityField;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SpecialEntityFieldCreateForm extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(SpecialEntityFieldCreateForm.class);

    public static String getApiPath() {
        return "/specialentityfield/v1/createForm";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static APIResponse getCreateFormResponse() {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(getApiPath(), getHeaders()).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static List<Integer> getAllPrimaryEntityTypeIdsFromResponse(String response) {
        try {
            JSONArray jsonArr = new JSONObject(response).getJSONObject("body").getJSONObject("data").getJSONObject("primaryEntity")
                    .getJSONObject("options").getJSONArray("data");

            return getAllEntityTypeIds(jsonArr);
        } catch (Exception e) {
            logger.error("Exception while Getting All Primary Entity Type Ids List from Response. " + e.getMessage());
            return null;
        }
    }

    public static List<Integer> getAllSecondaryEntityTypeIdsFromResponse(String response) {
        try {
            JSONArray jsonArr = new JSONObject(response).getJSONObject("body").getJSONObject("data").getJSONObject("secondaryEntity")
                    .getJSONObject("options").getJSONArray("data");

            return getAllEntityTypeIds(jsonArr);
        } catch (Exception e) {
            logger.error("Exception while Getting All Secondary Entity Type Ids List from Response. " + e.getMessage());
            return null;
        }
    }

    private static List<Integer> getAllEntityTypeIds(JSONArray jsonArr) {
        List<Integer> allEntityTypeIds = new ArrayList<>();

        for (int i = 0; i < jsonArr.length(); i++) {
            allEntityTypeIds.add(jsonArr.getJSONObject(i).getInt("id"));
        }

        return allEntityTypeIds;
    }
}