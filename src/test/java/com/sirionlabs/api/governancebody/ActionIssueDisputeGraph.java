package com.sirionlabs.api.governancebody;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class ActionIssueDisputeGraph extends TestAPIBase {

    static String actionqueryString="/listRenderer/list/227/gbIssueActionGraph/";
    static String issuequeryString="/listRenderer/list/228/gbIssueActionGraph/";
    static String disputequeryString="/listRenderer/list/491/gbDisputeGraph/";

    public static HashMap<String,String > getHeader()
    {
        return ApiHeaders.getDefaultLegacyHeaders();
    }


    public  static String actionGraph(int gbId)
    {
       String  queryString=actionqueryString+gbId;
        return  executor.get(queryString,getHeader()).getResponse().getResponseBody();
    }

    public  static String issueGraph(int gbId)
    {
        String  queryString=issuequeryString+gbId;
        return  executor.get(queryString,getHeader()).getResponse().getResponseBody();
    }

    public  static String disputeGraph(int gbId)
    {
        String queryString=disputequeryString+gbId;
        return  executor.get(queryString,getHeader()).getResponse().getResponseBody();
    }





}
