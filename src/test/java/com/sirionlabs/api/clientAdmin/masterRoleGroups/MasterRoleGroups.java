package com.sirionlabs.api.clientAdmin.masterRoleGroups;

import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;
import java.util.Map;

public class MasterRoleGroups extends TestAPIBase {

    public static String getAPIPath() {
        return "/masterrolegroups";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = ApiHeaders.getDefaultHeadersForClientAdminAPIs();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        return headers;
    }

    public static int  roleGroupCreateStatus(HashMap<String,String> payload)
    {
          return executor.post(getAPIPath(),getHeaders(),payload.toString()).getResponse().getResponseCode();
    }
}
