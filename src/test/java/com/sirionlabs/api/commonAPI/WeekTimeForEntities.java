package com.sirionlabs.api.commonAPI;

import java.util.HashMap;

public class WeekTimeForEntities {

    public static String getAPIPath(String createdEntityId) {
        return "/actionitemmgmts/show/" + createdEntityId;
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Accept-Encoding","gzip, deflate");
        return headers;
    }
}
