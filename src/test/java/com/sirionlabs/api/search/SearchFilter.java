package com.sirionlabs.api.search;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import java.util.HashMap;

public class SearchFilter extends TestAPIBase {

    public static String getNewApiPath() {

        return "/searchfilter/data";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public static APIResponse getSearchFilter() {
        APIResponse response = executor.get(getNewApiPath(), getHeaders()).getResponse();

        return response;
    }
}