package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class FetchDocument {

    public static String getAPIPath(String clientId,String docId,String filePath) {
        return "/autoExtraction/fetchDocument?clientId=" + clientId + "&docId="+ docId +"&filePath="+ filePath +"&statusUpdate=true";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getContentTypeAsJsonOnlyHeaderWithAuthorization();
    }
}
