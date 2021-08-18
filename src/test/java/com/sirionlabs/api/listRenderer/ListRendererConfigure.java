package com.sirionlabs.api.listRenderer;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ListRendererConfigure extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(ListRendererConfigure.class);
    private String listRendererConfigureJsonStr = null;


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();

        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Content-Type", "application/json;charset=UTF-8");

        return headers;
    }

    public static List<String> getAllColumnsSortedByOrderId(String configureResponse) {
        try {
            TreeMap<Integer, String> treeMap = getAllColumnsMapSortedByOrderId((configureResponse));

            if (treeMap == null)
                return null;

            return new ArrayList<>(treeMap.values());
        } catch (Exception e) {
            logger.error("Exception while Getting All Columns Sorted by Order Id. " + e.getMessage());
        }

        return null;
    }

    public static TreeMap<Integer, String> getAllColumnsMapSortedByOrderId(String configureResponse) {
        try {
            JSONObject jsonObj = new JSONObject(configureResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("columns");

            Map<Integer, String> hashMap = new HashMap<>();

            for (int i = 0; i < jsonArr.length(); i++) {
                hashMap.put(jsonArr.getJSONObject(i).getInt("order"), jsonArr.getJSONObject(i).getString("queryName"));
            }

            return new TreeMap<>(hashMap);
        } catch (Exception e) {
            logger.error("Exception while Getting All Columns Map Sorted by Order Id. " + e.getMessage());
        }

        return null;
    }

    public void hitListRendererConfigure(String urlId) {
        String queryString = "/listRenderer/list/" + urlId + "/configure";

        APIValidator apiValidator = executor.post(queryString, getHeaders(), "{}");
        APIResponse apiResponse = apiValidator.getResponse();
        this.listRendererConfigureJsonStr = apiResponse.getResponseBody();
    }

    public String hitListRendererConfigure(String urlId, CustomAssert customAssert) {

        String queryString = "/listRenderer/list/" + urlId + "/configure";
        APIValidator apiValidator = executor.post(queryString, getHeaders(), "{}");
        APIResponse apiResponse = apiValidator.getResponse();
        this.listRendererConfigureJsonStr = apiResponse.getResponseBody();
        apiValidator.validateResponseCode(200, customAssert);
        logger.debug("response json is: {}", listRendererConfigureJsonStr);

        logger.debug("API Status Code is : {}", apiResponse.getResponseCode());

        return listRendererConfigureJsonStr;
    }

    public String getListRendererConfigureJsonStr() {
        return listRendererConfigureJsonStr;
    }

    public void updateReportListConfigureResponse(int reportId, String payload, CustomAssert customAssert) {

        try {
            APIValidator apiValidator = executor.post("/listRenderer/list/" + reportId + "/listConfigureUpdate?reportName=undefined", getHeaders(), payload);
            APIResponse apiResponse = apiValidator.getResponse();

            apiValidator.validateResponseCode(200, customAssert);
            logger.debug("API Status Code for update is : {}", apiResponse.getResponseCode());
        } catch (Exception e) {
            logger.info("Exception Caught {}", e.getMessage());
            customAssert.assertTrue(false, "Exception Caught " + e.getMessage());
        }
    }

    public static int updateListConfigure(int listId, String payload) {
        try {
            return executor.post("/listRenderer/list/" + listId + "/listConfigureUpdate?reportName=undefined", getHeaders(), payload).getResponse().getResponseCode();
        } catch (Exception e) {
            logger.error("Exception while Updating List Configure for List Id: " + listId);
        }

        return -1;
    }
}