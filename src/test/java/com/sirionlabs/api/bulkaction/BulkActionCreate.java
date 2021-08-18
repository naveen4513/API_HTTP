package com.sirionlabs.api.bulkaction;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class BulkActionCreate {

    public static String getApiPath(int entityTypeId) {
        String url = "/bulkaction/create/" + entityTypeId;

        if (entityTypeId == 61) {
            url = url.concat("?version=2.0");
        }

        return url;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
}
