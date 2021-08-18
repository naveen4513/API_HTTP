package com.sirionlabs.api.plugin;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class PluginDownloadAPI extends TestAPIBase {
    String queryString;
    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getMeetingNoteCreateAPIHeader();
    }
    public APIResponse getPluginDownloadAPIResponse(int Id)
    {
        queryString="/plugin/download/wordplugin/"+Id;
        return  executor.get(queryString,getHeader()).getResponse();
    }
}
