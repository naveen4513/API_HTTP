package com.sirionlabs.api.governancebody;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class MeetingNoteFileUpload extends TestAPIBase {
    String queryString;
    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getMeetingNoteCreateAPIHeader();
    }
    public APIResponse getMeetingNoteFileUploadAPIResponse(int entityTypeId, int entityId, String payload)
    {
        queryString="/meetingnote/upload/"+entityTypeId+"/"+entityId;
        return  executor.post(queryString,getHeader(),payload).getResponse();
    }
}
