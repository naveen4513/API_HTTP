package com.sirionlabs.api.autoExtraction;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import java.util.HashMap;
public class PartialResetAPI extends TestAPIBase {
    public static String getAPIPath()
    {
        return "/autoextraction/partialRedo";
    }
    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
    public static APIResponse partialResetAPIResponse(String apiPath, HashMap<String, String> headers, String payload) {
        return executor.post(apiPath, headers, payload).getResponse();
    }

    public static String getPayload(String documentIds,String fieldIds)
    {
        return "{\"documentIds\":["+documentIds+"],\"fieldIds\":["+fieldIds+"]}";
    }
}
