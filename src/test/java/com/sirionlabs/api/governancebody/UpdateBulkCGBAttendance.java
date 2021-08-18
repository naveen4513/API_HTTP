package com.sirionlabs.api.governancebody;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class UpdateBulkCGBAttendance extends TestAPIBase {
    String queryString;
    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getContentTypeAsJsonOnlyHeader();
    }
    public APIResponse getUpdateAttendanceAPIResponse(int entityId, String payload)
    {
        queryString="/tblgovernancebodychild/updateBulkGCBParticipantsAttendance/"+entityId;
        return  executor.post(queryString,getHeader(),payload).getResponse();
    }
}
