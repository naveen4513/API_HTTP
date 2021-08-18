package com.sirionlabs.api.listRenderer;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class ListDataAPI extends TestAPIBase {

    public static String getApiPath(String listId, String contractId, String relationId, String vendorId, String isFirstCall, String version) {
        String apiPath = "/listRenderer/list/" + listId + "/listdata?contractId=" + contractId + "&relationId=" + relationId + "&vendorId=" + vendorId + "&isFirstCall=" +
                isFirstCall;

        if (version.contains("2.0")) {
            apiPath = apiPath.concat("&version=" + version);
        }

        return apiPath;
    }
    public  String getApiPath(int listId) {

        return "/listRenderer/list/" + listId + "/listdata";
    }


    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getPayload(String entityTypeId, String offset, String size, String orderByColumnName, String orderDirection, String filterJson,
                                    String selectedColumns) {
        return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName +
                "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":" + filterJson + "},\"selectedColumns\": " + selectedColumns + "}";
    }

    public HashMap<String, String> getHeader() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Accept-Encoding", "gzip, deflate");
        return headers;
    }

    public static APIResponse getListDataResponse(String apiPath, HashMap<String, String> headers, String payload) {
        return executor.post(apiPath, headers, payload).getResponse();
    }
    public  APIResponse getListDataResponseWithoutMandatoryHeaders(int listId,HashMap<String, String> headers,String payload) {
        return executor.postWithoutMandatoryHeaders(getApiPath(listId),headers , payload).getResponse();
    }
}