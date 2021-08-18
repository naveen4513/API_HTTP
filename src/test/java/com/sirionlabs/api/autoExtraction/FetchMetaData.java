package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class FetchMetaData {

    public static String getAPIPath(String clientid,String textid,String algoId) {
        return "/autoExtraction/fetchMetaData/" + clientid +"/" + textid + "/" + algoId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getContentTypeAsJsonOnlyHeader();
    }
}
