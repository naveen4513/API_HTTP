package com.sirionlabs.api.clientAdmin.EntityDumpReport;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ElasticDataDump extends TestAPIBase {
    public static String getApiPath() {
        return "/elasticdatadump";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("Accept-Language", "en-IN,en-US;q=0.9,en-GB;q=0.8,en;q=0.7");
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        headers.put("Accept-Encoding", "gzip, deflate");
        return headers;
    }

    public static APIResponse getElasticDataDumpResponse(Map<String,String> payload) throws UnsupportedEncodingException {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.postMultiPartFormData(getApiPath(), getHeaders(),payload).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}
