package com.sirionlabs.api.AllCommonAPI;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import org.apache.http.HttpResponse;

import java.util.HashMap;

public class APIExecutorCommon extends TestAPIBase {
    private String apiPath;
    private String payload;
    private String methodType;
    private String hostUrl;

    public APIExecutorCommon setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
        return this;
    }

    public APIExecutorCommon setMethodType(String methodType) {
        this.methodType = methodType;
        return this;
    }

    public APIExecutorCommon setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
        return this;
    }
    public APIExecutorCommon setPayload(String payload) {
        this.payload = payload;
        return this;
    }
    private HashMap<String,String> headers=new HashMap();
    public APIExecutorCommon setApiPath(String apiPath) {
        this.apiPath = apiPath;
        return this;
    }
    public APIExecutorCommon setHeaders(String key, String value) {
        this.headers.put(key,value);
        return this;
    }
    public HttpResponse getHttpResponseForGetAPI()
    {
        if (methodType.toLowerCase().equalsIgnoreCase("get"))
        {
            return executor.GetHttpResponseForGetAPI(apiPath,headers);
        }
        else
        {
           return null;
        }
    }
    public APIResponse getResponse()
    {
        if (methodType.toLowerCase().equalsIgnoreCase("get"))
        {
            return executor.get(apiPath,headers).getResponse();
        }
        else if (methodType.toLowerCase().equalsIgnoreCase("getwithhosturl"))
        {
            return executor.get("","",null).getResponse();
        }
        else if (methodType.toLowerCase().equalsIgnoreCase("post")) {
           return executor.post(apiPath.trim(), headers, payload).getResponse();
        }
        else
        {
            return null;
        }
    }
}
