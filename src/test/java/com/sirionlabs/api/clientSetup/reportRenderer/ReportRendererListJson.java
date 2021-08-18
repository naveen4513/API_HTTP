package com.sirionlabs.api.clientSetup.reportRenderer;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;

import java.util.HashMap;

public class ReportRendererListJson extends TestAPIBase {

    public static String getApiPath(int reportId, int clientId) {
        return "/reportRenderer/list/" + reportId + "/listJson?clientId=" + clientId;
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Content-Type", "application/json;charset=UTF-8");

        return headers;
    }

    public static String getListJsonResponse(int reportId, int clientId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        ClientSetupHelper setupHelperObj = new ClientSetupHelper();
        setupHelperObj.loginWithClientSetupUser();

        String response = executor.post(getApiPath(reportId, clientId), getHeaders(), null).getResponse().getResponseBody();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        return response;
    }
}