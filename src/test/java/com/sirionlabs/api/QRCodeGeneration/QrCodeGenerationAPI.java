package com.sirionlabs.api.QRCodeGeneration;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class QrCodeGenerationAPI extends TestAPIBase {
    public static String getApiPath() {
        return "/twoFactorAuthentication/v1/initAuthenticator";
    }
    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getOnlyHeaderWithAuthorization();
    }
    public static APIResponse getResponse(HashMap<String,String> headers)
    {
        APIResponse response = executor.getAPIWithoutMandatoryDefaultHeaders(getApiPath(), headers).getResponse();
        return response;
    }

    public static APIResponse getResponse()
    {
        APIResponse response = executor.getAPIWithoutMandatoryDefaultHeaders(getApiPath(), getHeaders()).getResponse();
        return response;
    }
}
