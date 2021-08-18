package com.sirionlabs.api.clientAdmin.masterslacategorys;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;
import java.util.Map;

public class CreateClientSLAItem extends TestAPIBase {

    public static String getApiPath() {
        return "/masterslaitems/createClientSLAItem";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String,String> header = new HashMap<>();
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        header.put("Accept", "text/html, */*; q=0.01");

        return header;
    }

    public static int postCreateClientSLAItem(Map<String,String> parameters) {

        int responseCode = -1;
        try {
            responseCode = executor.post(getApiPath(), getHeaders(),null, parameters).getResponse().getResponseCode();
        }catch (Exception e){

        }
        return responseCode;
    }

}
