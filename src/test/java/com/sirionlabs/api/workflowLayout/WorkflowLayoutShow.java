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

public class WorkflowLayoutShow extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(WorkflowLayoutShow.class);

    public static String getApiPath(int workflowLayoutId) {
        return "/workflowlayout/v1/show/" + workflowLayoutId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static APIResponse getWorkflowLayoutShowResponse(String apiPath, HashMap<String, String> headers) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(apiPath, headers).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static List<Integer> getAllEditPageTabIds(String showResponse) {
        return getAllMultiSelectFieldIdValues(showResponse, "editPageTabs");
    }

    public static List<Integer> getAllShowPageTabIds(String showResponse) {
        return getAllMultiSelectFieldIdValues(showResponse, "showPageTabs");
    }

    public static List<Integer> getAllEditableFieldsShowPageIds(String showResponse) {
        return getAllMultiSelectFieldIdValues(showResponse, "editableFieldsShowPage");
    }

    public static List<Integer> getAllEditableFieldsEditPageIds(String showResponse) {
        return getAllMultiSelectFieldIdValues(showResponse, "editableFieldsEditPage");
    }

    private static List<Integer> getAllMultiSelectFieldIdValues(String showResponse, String fieldObjectName) {
        List<Integer> allIds = new ArrayList<>();

        try {
            JSONArray jsonArr = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject(fieldObjectName).getJSONArray("values");

            for (int i = 0; i < jsonArr.length(); i++) {
                allIds.add(jsonArr.getJSONObject(i).getInt("id"));
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Multi Select Values of Field {}. {}", fieldObjectName, e.getMessage());
            return null;
        }

        return allIds;
    }
}