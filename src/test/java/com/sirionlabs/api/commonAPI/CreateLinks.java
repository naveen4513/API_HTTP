package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateLinks extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(CreateLinks.class);

    public static String getApiPath(int entityTypeId, int recordId) {
        return "/v2/createlinks/" + entityTypeId + "/" + recordId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getCreateLinksV2Response(int entityTypeId, int recordId) {
        return executor.get(getApiPath(entityTypeId, recordId), getHeaders()).getResponse().getResponseBody();
    }

    public static String getJSPAPIResponse(String apiPath) {
        return executor.get(apiPath, getHeaders()).getResponse().getResponseBody();
    }

    public static Map<Integer, String> getAllSingleCreateLinksMap(String createLinksV2Response) {
        Map<Integer, String> allCreateLinksMap = new HashMap<>();

        try {
            JSONObject jsonObj = new JSONObject(createLinksV2Response);
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject internalJsonObj = jsonArr.getJSONObject(i);

                if (internalJsonObj.has("fields") && !internalJsonObj.isNull("fields")) {
                    JSONArray internalJsonArr = internalJsonObj.getJSONArray("fields");

                    for (int j = 0; j < internalJsonArr.length(); j++) {
                        internalJsonObj = internalJsonArr.getJSONObject(j);

                        if (internalJsonObj.isNull("bulkSystemLabel")) {
                            if (!internalJsonObj.isNull("createEntityTypeId")) {
                                allCreateLinksMap.put(internalJsonObj.getInt("createEntityTypeId"), internalJsonObj.getString("jspApi"));
                            }
                        }
                    }
                } else {
                    if (!internalJsonObj.isNull("createEntityTypeId")) {
                        allCreateLinksMap.put(internalJsonObj.getInt("createEntityTypeId"), internalJsonObj.getString("jspApi"));
                    }
                }
            }

            return allCreateLinksMap;
        } catch (Exception e) {
            logger.error("Exception while Getting All Single Create Entity Links Map from CreateLinks V2 Response. " + e.getMessage());
            return null;
        }
    }

    public static String getCreateLinkForEntity(String createLinkResponse, int entityTypeId) {
        return getCreateLinkForEntity(createLinkResponse, entityTypeId, null);
    }

    public static String getCreateLinkForEntity(String createLinkResponse, int entityTypeId, String lineItemTypeId) {
        if (ParseJsonResponse.validJsonResponse(createLinkResponse) && !ParseJsonResponse.hasPermissionError(createLinkResponse)) {
            JSONObject jsonObj = new JSONObject(createLinkResponse);

            try {
                JSONArray jsonArr = jsonObj.getJSONArray("fields");

                if (entityTypeId == ConfigureConstantFields.getEntityIdByName("suppliers")) {
                    return jsonArr.getJSONObject(0).getString("jspApi");
                }

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    if (!jsonObj.isNull("createEntityTypeId")) {
                        int createEntityTypeId = jsonArr.getJSONObject(i).getInt("createEntityTypeId");

                        if (entityTypeId == createEntityTypeId) {
                            return jsonObj.getString("jspApi");
                        }
                    } else {
                        if (jsonObj.has("fields")) {
                            JSONArray internalJsonArr = jsonObj.getJSONArray("fields");

                            for (int j = 0; j < internalJsonArr.length(); j++) {
                                jsonObj = internalJsonArr.getJSONObject(j);

                                if (!jsonObj.isNull("createEntityTypeId")) {
                                    if (entityTypeId == jsonObj.getInt("createEntityTypeId")) {
                                        if (lineItemTypeId != null) {
                                            if (!jsonObj.getString("api").contains("lineItemTypeId=" + lineItemTypeId.trim())) {
                                                continue;
                                            }
                                        }

                                        return jsonObj.getString("jspApi");
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Exception while Getting Create Link for Entity Type Id {}. {}", entityTypeId, e.getStackTrace());
            }
        }

        return null;
    }

    public static Boolean hasCreateOptionForEntity(String createLinksResponse, String childEntityName) {
        try {
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(childEntityName);
            JSONObject jsonObj = new JSONObject(createLinksResponse);

            if (jsonObj.has("fields") && !jsonObj.isNull("fields")) {
                JSONArray jsonArr = jsonObj.getJSONArray("fields");

                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject internalJsonObj = jsonArr.getJSONObject(i);

                    if (internalJsonObj.has("fields") && !internalJsonObj.isNull("fields")) {
                        JSONArray internalJsonArr = internalJsonObj.getJSONArray("fields");

                        for (int j = 0; j < internalJsonArr.length(); j++) {
                            internalJsonObj = internalJsonArr.getJSONObject(j);

                            if (internalJsonObj.has("createEntityTypeId") && !internalJsonObj.isNull("createEntityTypeId") &&
                                    internalJsonObj.getInt("createEntityTypeId") == entityTypeId) {
                                return true;
                            }
                        }
                    } else {
                        if (internalJsonObj.getInt("createEntityTypeId") == entityTypeId) {
                            return true;
                        }
                    }
                }
            } else {
                return false;
            }

            return false;
        } catch (Exception e) {
            logger.error("Exception while Checking if Create Option is present or not in Show Response for Child Entity {}. {}", childEntityName, e.getStackTrace());
        }
        return null;
    }
}
