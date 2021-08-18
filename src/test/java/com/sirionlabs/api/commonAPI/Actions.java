package com.sirionlabs.api.commonAPI;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Actions extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(Actions.class);

    public static String getApiPath(int entityTypeId, int recordId) {
        return "/v2/actions/" + entityTypeId + "/" + recordId;
    }

    public static String getApiPathV3(int entityTypeId, int recordId) {
        return "/v3/actions/" + entityTypeId + "/" + recordId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getActionsV2Response(int entityTypeId, int recordId) {
        return executor.get(getApiPath(entityTypeId, recordId), getHeaders()).getResponse().getResponseBody();
    }

    public static String getActionsV3Response(int entityTypeId, int recordId) {
        return executor.get(getApiPathV3(entityTypeId, recordId), getHeaders()).getResponse().getResponseBody();
    }

    public static List<String> getAllActionNames(String actionsV2Response) {
        List<String> allActionNames = new ArrayList<>();

        try {
            JSONArray jsonArr = new JSONArray(actionsV2Response);

            for (int i = 0; i < jsonArr.length(); i++) {
                String actionName = jsonArr.getJSONObject(i).getString("name");
                allActionNames.add(actionName);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Action Names from Actions V2 Response. " + e.getMessage());
            return null;
        }

        return allActionNames;
    }

    public static List<String> getAllActionNamesV3(String actionsV3Response) {
        List<String> allActionNames = new ArrayList<>();

        try {
            JSONArray jsonArr = new JSONObject(actionsV3Response).getJSONArray("layoutActions");

            for (int i = 0; i < jsonArr.length(); i++) {
                String actionName = jsonArr.getJSONObject(i).getString("name");
                allActionNames.add(actionName);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Action Names from Actions V3 Response. " + e.getMessage());
            return null;
        }

        return allActionNames;
    }

    public static String getAPIForActionV3(String actionsV3Response, String actionName) {
        try {
            JSONArray jsonArr = new JSONObject(actionsV3Response).getJSONArray("layoutActions");

            for (int i = 0; i < jsonArr.length(); i++) {
                String actualActionName = jsonArr.getJSONObject(i).getString("name");

                if (actualActionName.equalsIgnoreCase(actionName)) {
                    return jsonArr.getJSONObject(i).getString("api");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting API for Action Name {} from Actions V3 Response. {}", actionName, e.getMessage());
        }

        return null;
    }

    public static String hitActionApi (String apiPath,String payload ) {
        APIResponse postResponse =  executor.post(apiPath,getHeaders(),payload).getResponse();
        return  postResponse.getResponseBody();
    }

    public static String hitActionApiGet (String apiPath) {
        APIResponse postResponse =  executor.get(apiPath,getHeaders()).getResponse();
        return  postResponse.getResponseBody();
    }
}