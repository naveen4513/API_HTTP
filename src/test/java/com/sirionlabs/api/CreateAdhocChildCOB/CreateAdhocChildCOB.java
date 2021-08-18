package com.sirionlabs.api.CreateAdhocChildCOB;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.APIUtils;

import java.util.HashMap;

public class CreateAdhocChildCOB extends TestAPIBase {
    String queryString="/tbldnoes/createAdhocChild";

    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
    public APIResponse getCreateAdhocChild(String payload)
    {
        return  executor.post(queryString,getHeader(),payload).getResponse();
    }
}
