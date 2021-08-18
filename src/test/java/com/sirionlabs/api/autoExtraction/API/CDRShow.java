package com.sirionlabs.api.autoExtraction.API;

import com.sirionlabs.api.commonAPI.Check;

import java.util.HashMap;

public class CDRShow
{
    public static String getAPIPath() {
        return "/cdr/show/";
    }

    public static HashMap<String, String> getHeaders()
    {
        HashMap<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Authorization", Check.getAuthorization());
        return headers;
    }

    public static String getMultiEntityAPIPath() {

        return "/autoextraction/document/metadata?entityTypeId=160&entityId=";

    }
}

