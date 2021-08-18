package com.sirionlabs.api.presignature;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.json.JSONObject;

import java.util.HashMap;

public class TagValidation extends TestAPIBase {

    public static String getApiPath() {
        return "/tagValidation";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getTagValidationResponse(String payload) {
        return executor.post(getApiPath(), getHeaders(), payload).getResponse().getResponseBody();
    }

    public static boolean isTagValidationSuccessful(String response) {
        try {
            JSONObject jsonObj = new JSONObject(response);
            return (jsonObj.isNull("errors") && jsonObj.getBoolean("success"));
        } catch (Exception e) {
            return false;
        }
    }
}