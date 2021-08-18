package com.sirionlabs.api.clientAdmin;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;

public class VendorDelete extends TestAPIBase {

    public static String getApiPath(int vendorId) {
        return "/vendors/delete?id=" + vendorId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static APIResponse getDeleteResponse(int vendorId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        APIResponse response = executor.delete(getApiPath(vendorId), getHeaders()).getResponse();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}