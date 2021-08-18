package com.sirionlabs.api.meetingnotedelete;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class MeetingNoteDelete extends TestAPIBase {
    public static String getApiPath() {
        return "/meetingnote/delete/";
    }
    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate");
        return headers;
    }
    public static APIResponse getResponse(String entityTypeId,String entityId,String documentId)
    {
        String apiPath=getApiPath()+entityTypeId+"/"+entityId+"/"+documentId;
        APIResponse response = executor.delete(apiPath, getHeaders()).getResponse();
        return response;
    }
}
