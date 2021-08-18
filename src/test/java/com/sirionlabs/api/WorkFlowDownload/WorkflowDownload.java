package com.sirionlabs.api.WorkFlowDownload;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.apache.http.HttpResponse;

import java.util.HashMap;

public class WorkflowDownload extends TestAPIBase {
    public static String getApiPath() {
        return "/workflow/download/";
    }
    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("accept-language","en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7");
        return headers;
    }
    public static HttpResponse getResponse(String workFlowId)
    {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();
        String apiPath=getApiPath()+workFlowId+"?dateFormatToShow=MM-dd-yyyy&dateFormatToPopulate=MM-dd-yyyy";
        HttpResponse response = executor.GetHttpResponseForGetAPI(apiPath, getHeaders());
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        return response;
    }
}
