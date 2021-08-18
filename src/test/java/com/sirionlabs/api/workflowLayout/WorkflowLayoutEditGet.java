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

public class WorkflowLayoutEditGet extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(WorkflowLayoutEditGet.class);

    public static String getApiPath(int workflowLayoutId) {
        return "/workflowlayout/v1/edit/" + workflowLayoutId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static APIResponse getEditGetResponse(String apiPath, HashMap<String, String> headers) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(apiPath, headers).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static List<Integer> getAllSelectedEditPageTabIds(String editGetResponse) {
        return getAllSelectedMultiSelectFieldIdValues(editGetResponse, "editPageTabs");
    }

    public static List<Integer> getAllSelectedShowPageTabIds(String editGetResponse) {
        return getAllSelectedMultiSelectFieldIdValues(editGetResponse, "showPageTabs");
    }

    public static List<Integer> getAllSelectedEditableFieldsShowPageIds(String editGetResponse) {
        return getAllSelectedMultiSelectFieldIdValues(editGetResponse, "editableFieldsShowPage");
    }

    public static List<Integer> getAllSelectedEditableFieldsEditPageIds(String editGetResponse) {
        return getAllSelectedMultiSelectFieldIdValues(editGetResponse, "editableFieldsEditPage");
    }

    public static List<Integer> getAllOptionsOfEditPageTabIds(String editGetResponse) {
        return getAllOptionsOfMultiSelectFieldIdValues(editGetResponse, "editPageTabs");
    }

    public static List<Integer> getAllOptionsOfShowPageTabIds(String editGetResponse) {
        return getAllOptionsOfMultiSelectFieldIdValues(editGetResponse, "showPageTabs");
    }

    public static List<Integer> getAllOptionsOfEditableFieldsShowPageIds(String editGetResponse) {
        return getAllOptionsOfMultiSelectFieldIdValues(editGetResponse, "editableFieldsShowPage");
    }

    public static List<Integer> getAllOptionsOfEditableFieldsEditPageIds(String editGetResponse) {
        return getAllOptionsOfMultiSelectFieldIdValues(editGetResponse, "editableFieldsEditPage");
    }

    private static List<Integer> getAllSelectedMultiSelectFieldIdValues(String editGetResponse, String fieldObjectName) {
        List<Integer> allIds = new ArrayList<>();

        try {
            JSONArray jsonArr = new JSONObject(editGetResponse).getJSONObject("body").getJSONObject("data").getJSONObject(fieldObjectName).getJSONArray("values");

            for (int i = 0; i < jsonArr.length(); i++) {
                allIds.add(jsonArr.getJSONObject(i).getInt("id"));
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Multi Select Values of Field {}. {}", fieldObjectName, e.getMessage());
            return null;
        }

        return allIds;
    }

    private static List<Integer> getAllOptionsOfMultiSelectFieldIdValues(String editGetResponse, String fieldObjectName) {
        List<Integer> allIds = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(editGetResponse).getJSONObject("body").getJSONObject("data").getJSONObject(fieldObjectName).getJSONObject("options");

            if (jsonObj.has("autoComplete") && jsonObj.getBoolean("autoComplete")) {
                //auto complete code missing.
            } else {
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < jsonArr.length(); i++) {
                    allIds.add(jsonArr.getJSONObject(i).getInt("id"));
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Multi Select Values of Field {}. {}", fieldObjectName, e.getMessage());
            return null;
        }

        return allIds;
    }
}