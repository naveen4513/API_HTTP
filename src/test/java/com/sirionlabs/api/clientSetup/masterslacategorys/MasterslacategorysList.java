package com.sirionlabs.api.clientSetup.masterslacategorys;

import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;
import java.util.Map;

public class MasterslacategorysList extends TestAPIBase {

    public static String getApiPath() {
        return "/masterslacategorys/list";
    }

    public static HashMap<String, String> getHeaders() {

        HashMap<String,String> header = new HashMap<>();
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        header.put("Accept", "text/html, */*; q=0.01");

        return header;
    }

    public static String getMasterSlaCategorysList(String hostUrl) {

        String responseBody = null;
        try {
            responseBody = executor.get(hostUrl,getApiPath(), getHeaders()).getResponse().getResponseBody();
            System.out.println();
        }catch (Exception e){

        }
        return responseBody;
    }

    public static String getMasterSlaCategorysList() {

        String responseBody = null;
        try {
            responseBody = executor.get(getApiPath(), getHeaders()).getResponse().getResponseBody();
            System.out.println();
        }catch (Exception e){

        }
        return responseBody;
    }

}
