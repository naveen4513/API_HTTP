package com.sirionlabs.api.workflowLayout;

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
import java.util.Map;

public class WorkflowLayoutCreateFormLayoutInfo extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(WorkflowLayoutCreateFormLayoutInfo.class);

    public static String getApiPath(int entityTypeId) {
        return "/workflowlayout/v1/createFormLayoutInfo/" + entityTypeId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static APIResponse getCreateFormLayoutInfoResponse(String apiPath, HashMap<String, String> headers) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(apiPath, headers).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static List<Map<String, String>> getAllOptionsOfEditPageTabs(String layoutInfoResponse) {
        return getAllOptionsOfMultiSelectFields(layoutInfoResponse, "editPageTabs");
    }

    public static List<Map<String, String>> getAllOptionsOfShowPageTabs(String layoutInfoResponse) {
        return getAllOptionsOfMultiSelectFields(layoutInfoResponse, "showPageTabs");
    }

    public static List<Map<String, String>> getAllOptionsOfEditableFieldsShowPage(String layoutInfoResponse) {
        return getAllOptionsOfMultiSelectFields(layoutInfoResponse, "editableFieldsShowPage");
    }

    public static List<Map<String, String>> getAllOptionsOfEditableFieldsEditPage(String layoutInfoResponse) {
        return getAllOptionsOfMultiSelectFields(layoutInfoResponse, "editableFieldsEditPage");
    }

    private static List<Map<String, String>> getAllOptionsOfMultiSelectFields(String layoutInfoResponse, String fieldObjectName) {
        List<Map<String, String>> allOptions = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(layoutInfoResponse).getJSONObject("body").getJSONObject("data").getJSONObject(fieldObjectName).getJSONObject("options");

            if (jsonObj.has("autoComplete") && jsonObj.getBoolean("autoComplete")) {
                //auto complete code missing.
            } else {
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    if (jsonObj.has("name") && !jsonObj.isNull("name")) {
                        Map<String, String> optionMap = new HashMap<>();

                        optionMap.put("id", String.valueOf(jsonObj.getInt("id")));
                        optionMap.put("name", jsonObj.getString("name"));

                        allOptions.add(optionMap);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Multi Select Values of Field {}. {}", fieldObjectName, e.getMessage());
            return null;
        }

        return allOptions;
    }
}