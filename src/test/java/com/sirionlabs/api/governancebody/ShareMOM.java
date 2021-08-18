package com.sirionlabs.api.governancebody;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class ShareMOM extends TestAPIBase {

    static String queryString;

    public static HashMap<String,String > getHeader()
    {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
    public  static APIResponse getShareMom(int cgbId)
    {
        queryString="/tblgovernancebodychild/createShareForm/"+cgbId;
        return  executor.get(queryString,getHeader()).getResponse();
    }

    public  static APIResponse postShareMom(int cgbId, String payload)
    {
        queryString="/tblgovernancebodychild/shareMOM/"+cgbId;
        return  executor.post(queryString,getHeader(),payload).getResponse();
    }


    public static String getPostShareMOMPayloadTemplate(){
        String payload = "{\"file\":null,\"maxEmailCount\":20,\"canShareExternalUser\":true,\"to\":null,\"toExternal\":null,\"subject\":null,\"attachmentName\":null,\"meetingMinutesHtml\":\"<html></html>\",\"comments\":\"comment\",\"shareDate\":null,\"selectedUsers\":null}";
        return payload;
    }



}
