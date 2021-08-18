package com.sirionlabs.api.clientAdmin.dynamicMetadata;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

/**
 * Created by nikhil.haritash on 18-04-2019.
 */
public class DynamicMetadataShow {
    public static String getAPIPath(String fieldId) {
        return "/dynamicMetadata/show/" + fieldId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }
}
