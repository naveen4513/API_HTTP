package com.sirionlabs.api.bulkRevalidate;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class BulkRevalidate extends TestAPIBase {

    public static String getApiPath() {
        return "/bulk-revalidate/save";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static APIResponse bulkRevalidate(String payload)
    {
        APIResponse response = executor.post(getApiPath(), getHeaders(),payload).getResponse();

        return response;
    }
}