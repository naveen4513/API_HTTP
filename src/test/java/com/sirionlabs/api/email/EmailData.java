package com.sirionlabs.api.email;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.email.TestEmailAction;

import java.util.HashMap;

public class EmailData extends TestAPIBase {

    public static String getNewApiPath(String entityTypeId, String emailAction, String languageId) {

        return "/tblsystemEmailConfigurations/emaildata?entityTypeId="+entityTypeId+"&emailAction="+emailAction+"&languageId="+languageId+"";
    }


    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }


    public static APIResponse postEmailActionAPI(String apiPath, HashMap<String, String> headers, String payload) {
        APIResponse response = executor.post(apiPath, headers, payload).getResponse();

        return response;
    }
}