package com.sirionlabs.api.governancebody;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class PublishMOM extends TestAPIBase {
    String queryString;

    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
    public APIResponse getPublishMOM(int entityId, String payload)
    {
        queryString="/tblgovernancebodychild/publishMOM/"+entityId;
        return  executor.post(queryString,getHeader(),payload).getResponse();
    }

}
