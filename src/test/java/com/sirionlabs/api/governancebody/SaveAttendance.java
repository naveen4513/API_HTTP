package com.sirionlabs.api.governancebody;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class SaveAttendance extends TestAPIBase {
    String queryString;
    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getMeetingNoteCreateAPIHeader();
    }
    public APIResponse getSaveAttendance(int entityId, String payload)
    {
        queryString="/tblgovernancebodychild/saveAttendance/"+entityId;
        return  executor.post(queryString,getHeader(),payload).getResponse();
    }
}
