package com.sirionlabs.api.clientAdmin.masterRoleGroups;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class MasterRoleGroupSubHeader extends TestAPIBase {
    String queryString;

    public HashMap<String,String > getHeader()
    {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept", "text/html, */*; q=0.01");
        headers.put("Accept-Language","en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7");
        return headers;
    }
    public APIResponse getMasterRoleGroupHeader(int entityId)
    {
        queryString="/masterrolegroups/subheaders?"+entityId;
        return  executor.get(queryString,getHeader()).getResponse();
    }
}
