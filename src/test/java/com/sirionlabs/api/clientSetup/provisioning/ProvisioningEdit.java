package com.sirionlabs.api.clientSetup.provisioning;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;

import java.util.HashMap;
import java.util.Map;

public class ProvisioningEdit extends TestAPIBase {

    public static String getApiPath(int clientId, String clientName) {
        Map<String, String> parametersMap = new HashMap<>();
        parametersMap.put("clientId", String.valueOf(clientId));
        parametersMap.put("clientName", clientName);

        String params = UrlEncodedString.getUrlEncodedString(parametersMap);
        return "/provisioning/edit/" + clientId + "?" + params;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static String getProvisioningEditResponse(int clientId, String clientName) {
        return executor.get(getApiPath(clientId, clientName), getHeaders()).getResponse().getResponseBody();
    }
}