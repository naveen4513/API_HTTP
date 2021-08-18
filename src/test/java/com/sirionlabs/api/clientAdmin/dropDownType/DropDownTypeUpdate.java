package com.sirionlabs.api.clientAdmin.dropDownType;

import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

/**
 * Created by nikhil.haritash on 25-04-2019.
 */
public class DropDownTypeUpdate extends TestAPIBase {

    public static String getAPIPath() {
        return "/dropdowntype/update";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html, */*; q=0.01");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept-Encoding", "gzip, deflate");

        return headers;
    }


    public static HashMap<String, String> getParameters(int size, String FieldType, String Id) {
        HashMap<String, String> params = new HashMap<>();

        params.put("size",String.valueOf(size));
        params.put("dropdownType",FieldType);
        if (FieldType.equalsIgnoreCase("autocomplete")) {
            params.put("_enableListView", "on");
            params.put("enableListView", "true");
        }
        params.put("id",Id);
        params.put("ajax","true");
        params.put("history", "{}");

        return params;
    }

    public int hitDropdownAPI(int size,String fieldType,String id){

        String apiPath = DropDownTypeUpdate.getAPIPath();
        HashMap<String,String> headersForCreate = DropDownTypeUpdate.getHeaders();
        HashMap<String,String> params = DropDownTypeUpdate.getParameters(size, fieldType,id);

        int responseCode = -1;

        try {
            responseCode = executor.postMultiPartFormData(apiPath, headersForCreate, params).getResponse().getResponseCode();
        }catch (Exception e){
            responseCode = -1;
        }
        return responseCode;
    }


}
