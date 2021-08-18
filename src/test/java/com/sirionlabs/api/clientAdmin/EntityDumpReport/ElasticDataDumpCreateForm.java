package com.sirionlabs.api.clientAdmin.EntityDumpReport;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class ElasticDataDumpCreateForm extends TestAPIBase {
    public static String getApiPath() {
        return "/elasticdatadump/createForm";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html, */*; q=0.01");
        headers.put("Accept-Language", "en-IN,en-US;q=0.9,en-GB;q=0.8,en;q=0.7");

        return headers;
    }

    public static APIResponse getElasticDataDumpCreateFormResponse() {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.get(getApiPath(), getHeaders()).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}
