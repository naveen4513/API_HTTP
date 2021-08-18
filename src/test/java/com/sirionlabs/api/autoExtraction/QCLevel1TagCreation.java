package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.json.JSONObject;
import org.testng.SkipException;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class QCLevel1TagCreation extends TestAPIBase {
    public static String getAPIPath()
    {
        return "/metadataautoextraction/create/4";
    }
    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getPayload() {
        String qcLevel1Tag = "Tag1" + RandomString.getRandomAlphaNumericString(5);
        return "{\"name\":\""+qcLevel1Tag+"\"}";
    }
    public static APIResponse tag1CreateAPIResponse(String apiPath, HashMap<String, String> headers, String payload) {
        return executor.post(apiPath, headers, payload).getResponse();
    }
}
