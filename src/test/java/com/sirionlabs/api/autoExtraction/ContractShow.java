package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.api.commonAPI.Check;

import java.util.HashMap;

public class ContractShow {

    public static String getAPIPath() {
        return "/contracts/show/";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Authorization", Check.getAuthorization());
        return headers;
    }

    public static String getMultiEntityAPIPath() {

        return "/autoextraction/document/metadata?entityTypeId=61&entityId=";

    }
}
