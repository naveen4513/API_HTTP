package com.sirionlabs.api.plugin;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class PluginInfoAPI extends TestAPIBase {
    String queryString;
    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getMeetingNoteCreateAPIHeader();
    }
    public APIResponse getPluginInfoAPIResponse(int Id)
    {
        queryString="/plugin/download/latestVersionInfo/"+Id;
        return  executor.get(queryString,getHeader()).getResponse();
    }
}
