package com.sirionlabs.api.clientAdmin.masterUserRoleGroups;

import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class MasterUserRoleGroupsUpdate extends TestAPIBase {

    public static String getApiPath(Integer roleGroupId) {
        return "/masteruserrolegroups/update/" + roleGroupId+"?ajax=true";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("Accept-Encoding", "gzip, deflate");

        return headers;
    }

    public static String getUpdateResponse(int roleGroupId) {
        return executor.get(getApiPath(roleGroupId), getHeaders()).getResponse().getResponseBody();
    }
}