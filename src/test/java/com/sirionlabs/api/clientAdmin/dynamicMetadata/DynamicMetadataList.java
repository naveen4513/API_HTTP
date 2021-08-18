package com.sirionlabs.api.clientAdmin.dynamicMetadata;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

/**
 * Created by nikhil.haritash on 17-04-2019.
 */
public class DynamicMetadataList {

    private static String apiPath = "/dynamicMetadata/list";

    public static String getApiPath() {
        return apiPath;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }
}
