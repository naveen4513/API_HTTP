package com.sirionlabs.api.governancebody;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class MeetingNoteGetDocumentListAPI extends TestAPIBase {
    String queryString;
    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
    public APIResponse getResponse(int entityTypeId, int entityId)
    {
        queryString="/meetingnote/getList/"+entityTypeId+"/"+entityId;
        return executor.get(queryString,getHeader()).getResponse();
    }
}
