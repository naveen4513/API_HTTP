package com.sirionlabs.api.governancebody;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class MeetingNoteCreateAPI extends TestAPIBase {
    String queryString;
    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getMeetingNoteCreateAPIHeader();
    }
    public APIResponse getMeetingNoteCreateAPIResponse(int entityTypeId,int entityId,String payload)
    {
            queryString="/meetingnote/create/"+entityTypeId+"/"+entityId;
          return  executor.post(queryString,getHeader(),payload).getResponse();
    }

}
