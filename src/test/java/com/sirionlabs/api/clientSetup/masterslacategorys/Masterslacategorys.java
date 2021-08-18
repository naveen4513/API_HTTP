package com.sirionlabs.api.clientSetup.masterslacategorys;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;
import java.util.Map;

public class Masterslacategorys extends TestAPIBase {

    public static String getApiPath() {
        return "/masterslacategorys";
    }

    public static HashMap<String, String> getHeaders() {

        HashMap<String,String> header = new HashMap<>();
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        header.put("Accept", "text/html, */*; q=0.01");

        return header;
    }

    public static int postMasterSlaCategorys(String hostUrl,Map<String,String> parameters) {

        String responseBody = null;
        int responseCode = -1;
        try {
            responseCode = executor.post(hostUrl,getApiPath(), getHeaders(), null,parameters).getResponse().getResponseCode();
            System.out.println();
        }catch (Exception e){

        }
        return responseCode;
    }

}
