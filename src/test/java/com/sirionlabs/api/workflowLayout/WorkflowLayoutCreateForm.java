package com.sirionlabs.api.workflowLayout;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.test.api.workflowPod.workflowLayout.TestWorkflowLayoutCreateFormAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WorkflowLayoutCreateForm extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(WorkflowLayoutCreateForm.class);

    public static String getApiPath() {
        return "/workflowlayout/v1/createForm";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static APIResponse getCreateFormResponse(String apiPath, HashMap<String, String> headers) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(apiPath, headers).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }

    public static List<Integer> getAllEntityTypeIdsFromResponse(String response) {
        List<Integer> allEntityTypeIds = new ArrayList<>();

        try {
            JSONArray jsonArr = new JSONObject(response).getJSONArray("data");

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