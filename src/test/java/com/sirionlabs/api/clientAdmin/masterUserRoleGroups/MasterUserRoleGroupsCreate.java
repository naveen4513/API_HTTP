package com.sirionlabs.api.clientAdmin.masterUserRoleGroups;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class MasterUserRoleGroupsCreate {

    public static String getAPIPath() {
        return "/masteruserrolegroups/create";
    }

    public static String getCreateAPIPath() {
        return "/masteruserrolegroups";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = ApiHeaders.getDefaultHeadersForClientAdminAPIs();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        return headers;
    }
    public static String getShowAPIPath() {
        return "/masteruserrolegroups/show";
    }

    public static String postUpdateAPIPath() {
        return "/masteruserrolegroups/update";
    }
}